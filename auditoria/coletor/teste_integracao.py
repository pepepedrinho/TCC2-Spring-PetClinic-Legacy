"""Exercita o painel contra JVMs locais e bancos descartáveis."""
import json
import os
from pathlib import Path
import re
import socket
import subprocess
from tempfile import TemporaryDirectory
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
MODULES = {
    "legacy": ("monolith", "spring-petclinic-1.5.1.jar", "JAVA8_HOME"),
    "owners": ("owners-service", "owners-service-1.0.0-SNAPSHOT.jar", "JAVA17_HOME"),
    "gateway": ("gateway", "petclinic-gateway-1.0.0-SNAPSHOT.jar", "JAVA17_HOME"),
}


def free_ports(count):
    listeners = [socket.socket() for _ in range(count)]
    try:
        for listener in listeners:
            listener.bind(("127.0.0.1", 0))
        return [listener.getsockname()[1] for listener in listeners]
    finally:
        for listener in listeners:
            listener.close()


def origin(port):
    return f"http://127.0.0.1:{port}"


def get(port, path, expected=200, method="GET"):
    try:
        with urlopen(Request(origin(port) + path, method=method), timeout=10) as response:
            assert response.status == expected, (path, response.status, expected)
            return response.read().decode()
    except HTTPError as error:
        with error:
            assert error.code == expected, (path, error.code, expected)
            return error.read().decode()


def wait_until(check, seconds=90):
    deadline = time.monotonic() + seconds
    while True:
        try:
            return check()
        except (AssertionError, URLError, TimeoutError):
            if time.monotonic() >= deadline:
                raise
            time.sleep(0.5)


def run(folder):
    legacy, owners, gateway, legacy_admin, owners_admin, gateway_admin, panel = free_ports(7)
    env = {
        **os.environ,
        "LEGACY_DB_URL": f"jdbc:hsqldb:file:{folder.as_posix()}/legacy;shutdown=true",
        "OWNERS_DB_URL": f"jdbc:h2:file:{folder.as_posix()}/owners;DB_CLOSE_ON_EXIT=FALSE",
        "MONOLITH_URL": origin(legacy), "MONOLITH_ROUTE_URI": origin(legacy),
        "OWNERS_URL": origin(owners), "OWNERS_ROUTE_URI": origin(owners),
        "AUDIT_MANAGEMENT_ADDRESS": "127.0.0.1",
        "AUDIT_LEGACY_MANAGEMENT_PORT": str(legacy_admin),
        "AUDIT_OWNERS_MANAGEMENT_PORT": str(owners_admin),
        "AUDIT_GATEWAY_MANAGEMENT_PORT": str(gateway_admin),
        "AUDIT_LEGACY_URL": origin(legacy_admin), "AUDIT_OWNERS_URL": origin(owners_admin),
        "AUDIT_GATEWAY_URL": origin(gateway_admin), "AUDIT_HOST": "127.0.0.1",
        "AUDIT_PORT": str(panel), "AUDIT_INTERVAL_MS": "5000",
    }
    processes = {}
    logs = {}

    def start(name, command, **overrides):
        path = folder / (name + ".log")
        logs[name] = path
        with path.open("ab") as log:
            processes[name] = subprocess.Popen(command, cwd=ROOT, env={**env, **overrides},
                                               stdout=log, stderr=subprocess.STDOUT)

    def start_java(name, port, profiles, **overrides):
        module, jar_name, java_home = MODULES[name]
        java = Path(os.environ[java_home]) / "bin" / ("java.exe" if os.name == "nt" else "java")
        jar = ROOT / "projects" / module / "target" / jar_name
        assert jar.is_file(), f"Execute o build antes do teste: {jar}"
        start(name, [str(java), "-jar", str(jar), f"--server.port={port}",
                     f"--spring.profiles.active={profiles}"], **overrides)

    def stop(name):
        process = processes.pop(name)
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=20)

    def observation(states, metrics_state=None):
        data = json.loads(get(panel, "/api/observacao"))
        assert [item["state"] for item in data["status"]] == states, data["status"]
        if metrics_state is not None:
            assert data["metrics"]["state"] == metrics_state, data["metrics"]
        return data

    try:
        start_java("legacy", legacy, "production,auditoria", SPRING_DATASOURCE_INITIALIZE="true")
        start_java("owners", owners, "auditoria")
        start_java("gateway", gateway, "auditoria")
        start("panel", ["node", "auditoria/coletor/server.mjs"])
        cold = wait_until(lambda: observation(["up", "up", "up"], "empty"))
        routes = {route["id"]: route for route in cold["routes"]["items"]}
        assert routes["petclinic-monolith"]["paths"] == "/**"
        assert routes["petclinic-monolith"]["destination"] == origin(legacy)
        assert routes["owners-service"]["destination"] == origin(owners)
        assert routes["internal-block"]["destination"] == "Bloqueio local"
        assert "Auditoria PetClinic" in get(panel, "/index.html")
        assert ".down" in get(panel, "/style.css")
        assert "refresh()" in get(panel, "/app.js")
        get(panel, "/api/observacao?url=http://localhost", 404)
        get(panel, "/api/observacao", 405, "POST")
        get(panel, "/actuator/gateway/routes", 404)

        browser = os.environ.get("AUDIT_BROWSER")
        if browser:
            result = subprocess.run(
                [browser, "--headless", "--disable-gpu", "--no-first-run",
                 f"--user-data-dir={folder / 'browser'}", "--virtual-time-budget=8000",
                 "--dump-dom", origin(panel)],
                capture_output=True, encoding="utf-8", errors="replace", timeout=30, check=True,
            )
            assert len(re.findall(r"<strong class=\"up\">Ativo</strong>", result.stdout)) == 3, result.stdout[-3000:]
            assert "Sem amostras registradas." in result.stdout
            print("JavaScript executado no navegador.")

        get(gateway, "/")
        get(gateway, "/api/owners/1")
        get(gateway, "/api/owners/9999999", 404)
        traffic = wait_until(lambda: observation(["up", "up", "up"], "ok"))["metrics"]
        assert traffic["count"] >= 3 and traffic["meanMs"] >= 0, traffic
        assert {item["id"] for item in traffic["byRoute"]} >= {"petclinic-monolith", "owners-service"}
        assert {item["code"] for item in traffic["byCode"]} >= {"200", "404"}

        get(gateway, "/actuator/gateway/routes", 404)
        get(gateway, "/actuator/metrics", 404)
        get(gateway, "/manage/health", 404)
        get(legacy, "/manage/health", 404)
        get(owners, "/actuator/health", 404)
        get(legacy_admin, "/manage/env", 404)
        get(owners_admin, "/actuator/env", 404)
        get(gateway_admin, "/actuator/env", 404)
        get(gateway_admin, "/actuator/gateway/refresh", 404, "POST")

        stop("owners")
        wait_until(lambda: observation(["up", "up", "down"], "ok"), 30)
        get(gateway, "/api/owners/1", 503)
        get(gateway, "/")
        start_java("owners", owners, "auditoria")
        wait_until(lambda: observation(["up", "up", "up"], "ok"))
        get(gateway, "/api/owners/1")
        stop("gateway")
        offline = wait_until(lambda: observation(["down", "up", "up"], "unavailable"), 30)
        assert offline["routes"]["state"] == "unavailable"
        start_java("gateway", gateway, "auditoria")
        wait_until(lambda: observation(["up", "up", "up"], "empty"))
        stop("panel")
        get(gateway, "/")
        print("Integração da auditoria aprovada.")
    except BaseException:
        for name, path in logs.items():
            print(name, "\n".join(path.read_text(errors="replace").splitlines()[-15:]), sep="\n")
        raise
    finally:
        for name in list(processes):
            stop(name)


if __name__ == "__main__":
    with TemporaryDirectory(prefix="petclinic-audit-") as directory:
        run(Path(directory))

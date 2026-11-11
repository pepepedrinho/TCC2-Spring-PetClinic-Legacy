window.dashAgGridFunctions = window.dashAgGridFunctions || {};
window.dashAgGridFunctions.formatClassName = function (params) {
    const name = params.value ?? "";
    return name.slice(name.lastIndexOf(".") + 1);
};

window.dashAgGridFunctions.formatMetric = function (params) {
    if (params.value === null || params.value === undefined) return "—";
    return new Intl.NumberFormat("pt-BR", {maximumFractionDigits: 6}).format(params.value);
};

document.addEventListener("click", function (event) {
    const button = event.target.closest(".fullscreen-button");
    if (!button) return;
    if (document.fullscreenElement) {
        document.exitFullscreen();
    } else if (button.parentElement.requestFullscreen) {
        button.parentElement.requestFullscreen();
    }
});
document.addEventListener("fullscreenchange", function () {
    for (const button of document.querySelectorAll(".fullscreen-button")) {
        button.textContent = button.parentElement === document.fullscreenElement ? "Sair da tela cheia" : "Tela cheia";
    }
});

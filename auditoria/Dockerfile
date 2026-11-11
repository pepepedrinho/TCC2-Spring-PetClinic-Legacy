FROM node:22-alpine
WORKDIR /app
COPY coletor/server.mjs coletor/server.mjs
COPY painel/ painel/
USER node
EXPOSE 3000
CMD ["node", "coletor/server.mjs"]

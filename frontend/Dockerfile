FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent
COPY . .
ENV SKIP_PREFLIGHT_CHECK=true
EXPOSE 3000
CMD ["npm", "start"]
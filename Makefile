.DEFAULT_GOAL := help

ROOT_DIR := $(abspath .)
PYTHON ?= python3
COMPOSE ?= docker compose

APP_COMPOSE := docker-compose.app.yml
PIPELINE_COMPOSE := docker-compose.pipeline.yml

.PHONY: help \
	dev dev-backend dev-frontend \
	fetch \
	test backend-test crawler-test \
	docker-build buildapp buildpipeline docker-up docker-down docker-pipeline \
	deployapp package-app submit-app

# -----------------------------------------------------------------------------
# Development
# -----------------------------------------------------------------------------

help: ## Show available targets
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z0-9_-]+:.*## / {printf "  %-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

dev: ## Start the backend and frontend in separate Terminal windows
	@$(MAKE) dev-backend
	@$(MAKE) dev-frontend

dev-backend: ## Start the backend in a separate Terminal window
	@osascript -e 'tell application "Terminal" to do script "cd $(ROOT_DIR)/Backend && ./mvnw spring-boot:run"'

dev-frontend: ## Start the frontend in a separate Terminal window
	@osascript -e 'tell application "Terminal" to do script "cd $(ROOT_DIR)/Frontend && pnpm start"'

# -----------------------------------------------------------------------------
# Data pipeline
# -----------------------------------------------------------------------------

fetch: ## Run the crawler, then the analyser
	@$(PYTHON) $(ROOT_DIR)/Crawler/main.py
	@$(PYTHON) $(ROOT_DIR)/Analyser/main.py

# -----------------------------------------------------------------------------
# Tests
# -----------------------------------------------------------------------------

test: ## Run backend and crawler tests
	@$(MAKE) backend-test
	@$(MAKE) crawler-test

backend-test: ## Run backend tests
	@cd Backend && ./mvnw test

crawler-test: ## Run crawler tests
	@cd Crawler && $(PYTHON) -m pytest tests/ -v

# -----------------------------------------------------------------------------
# Docker
# -----------------------------------------------------------------------------

docker-build: buildapp buildpipeline ## Build application and pipeline images

buildapp: ## Build application images
	@$(COMPOSE) -f $(APP_COMPOSE) build

buildpipeline: ## Build pipeline image
	@$(COMPOSE) -f $(PIPELINE_COMPOSE) build

docker-up: ## Start application containers
	@$(COMPOSE) -f $(APP_COMPOSE) up -d

docker-down: ## Stop application containers
	@$(COMPOSE) -f $(APP_COMPOSE) down

docker-pipeline: ## Run the crawler/analyser pipeline container
	@$(COMPOSE) -f $(PIPELINE_COMPOSE) run --rm pipeline

# -----------------------------------------------------------------------------
# Deployment
# -----------------------------------------------------------------------------

deployapp: buildapp package-app submit-app ## Build, package, and upload app images

package-app: ## Package application images into a gzip archive
	@docker save \
		infomoth-frontend:latest \
		infomoth-backend:latest \
		| gzip > infomoth-app-images.tar.gz

submit-app: ## Upload the application archive and compose file
	@scp infomoth-app-images.tar.gz imapp:~/InfoMoth/
	@scp $(APP_COMPOSE) imapp:~/InfoMoth/

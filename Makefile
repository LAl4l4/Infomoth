.DEFAULT_GOAL := help

ROOT_DIR := $(abspath .)
PYTHON ?= python3
COMPOSE ?= docker compose

APP_COMPOSE := docker-compose.app.yml
APP_CONFIG := Config/deploy-config.json
APP_ARCHIVE := infomoth-app-images.tar.gz
APP_HOST ?= imapp
APP_REMOTE_DIR ?= /home/ubuntu/InfoMoth
DATABASE_SCHEMA_DIR := Schema
PIPELINE_COMPOSE := docker-compose.pipeline.yml
PIPELINE_ARCHIVE := infomoth-pipeline-image.tar.gz
PIPELINE_HOST ?= impipe
PIPELINE_REMOTE_DIR ?= /home/ubuntu/InfoMoth

.PHONY: help \
	dev dev-backend dev-frontend \
	fetch \
	test backend-test crawler-test analyser-test \
	docker-build buildapp buildpipeline docker-up docker-down docker-pipeline \
	deployapp deploypipe package-app package-pipe submit-app submit-pipe submit-models start-app start-pipe

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

test: ## Run frontend, backend, crawler, and analyser tests
	@$(MAKE) frontend-test
	@$(MAKE) backend-test
	@$(MAKE) crawler-test
	@$(MAKE) analyser-test

frontend-test: ## Run frontend tests
	@cd Frontend && pnpm test

backend-test: ## Run backend tests
	@cd Backend && ./mvnw test

crawler-test: ## Run crawler tests
	@cd Crawler && $(PYTHON) -m pytest tests/ -v

analyser-test: ## Run analyser tests
	@cd Analyser && $(PYTHON) -m pytest tests/ -v

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

docker-pipeline: ## Start the hourly crawler/analyser pipeline container
	@$(COMPOSE) -f $(PIPELINE_COMPOSE) up -d pipeline

# -----------------------------------------------------------------------------
# Deployment
# -----------------------------------------------------------------------------

deployapp: buildapp package-app submit-app start-app ## Build, package, upload, and start the app

deploypipe: buildpipeline package-pipe submit-pipe start-pipe ## Build, package, upload, and start the pipeline

package-app: ## Package application images into a gzip archive
	@docker save \
		infomoth-frontend:latest \
		infomoth-backend:latest \
		| gzip > $(APP_ARCHIVE)

package-pipe: ## Package the pipeline image into a gzip archive
	@docker save infomoth-pipeline:latest | gzip > $(PIPELINE_ARCHIVE)

submit-app: ## Upload the application archive and compose file
	@ssh $(APP_HOST) 'mkdir -p $(APP_REMOTE_DIR)/Config $(APP_REMOTE_DIR)/Shared $(APP_REMOTE_DIR)/scripts'
	@scp $(APP_ARCHIVE) $(APP_COMPOSE) $(APP_HOST):$(APP_REMOTE_DIR)/
# Config and database schemas are runtime files and are uploaded separately.
	@scp $(APP_CONFIG) $(APP_HOST):$(APP_REMOTE_DIR)/Config/deploy-config.json
	@scp -r $(DATABASE_SCHEMA_DIR) $(APP_HOST):$(APP_REMOTE_DIR)/
	@scp scripts/fix_sentiment_duplicates.sql $(APP_HOST):$(APP_REMOTE_DIR)/scripts/

start-app: ## Load and start the uploaded application images on the App VM
	@ssh $(APP_HOST) 'cd $(APP_REMOTE_DIR) && gzip -dc $(APP_ARCHIVE) | docker load && docker compose -f $(APP_COMPOSE) up -d --no-build && docker compose -f $(APP_COMPOSE) ps'

submit-pipe: ## Upload the pipeline archive, compose file, and runtime config
	@ssh $(PIPELINE_HOST) 'mkdir -p $(PIPELINE_REMOTE_DIR)/Config $(PIPELINE_REMOTE_DIR)/Shared'
	@scp $(PIPELINE_ARCHIVE) $(PIPELINE_COMPOSE) $(PIPELINE_HOST):$(PIPELINE_REMOTE_DIR)/
	@scp $(APP_CONFIG) $(PIPELINE_HOST):$(PIPELINE_REMOTE_DIR)/Config/deploy-config.json

submit-models: ## Incrementally upload local FinBERT models to the Pipeline VM
	@test -d models
	@ssh $(PIPELINE_HOST) 'mkdir -p $(PIPELINE_REMOTE_DIR)/models'
	@rsync -az --partial --progress models/ $(PIPELINE_HOST):$(PIPELINE_REMOTE_DIR)/models/

start-pipe: ## Load and start the uploaded pipeline image on the Pipeline VM
	@ssh $(PIPELINE_HOST) 'cd $(PIPELINE_REMOTE_DIR) && gzip -dc $(PIPELINE_ARCHIVE) | docker load -i infomoth-pipeline-image.tar.gz && docker compose -f $(PIPELINE_COMPOSE) up -d --no-build pipeline && docker compose -f $(PIPELINE_COMPOSE) ps pipeline'

#!/usr/bin/env bash
# Development helper script for spring-boot-repo
# Usage: ./scripts/dev.sh [command]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Directories
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AZURE_PLATFORM="${REPO_ROOT}/azure-java-platform/azure-java-platform"
FARM_WORKERS="${REPO_ROOT}/farm-workers-api/farm-workers-api"
SPRING_ACADEMY="${REPO_ROOT}/spring-academy-intro"

# Helper functions
log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Commands
help() {
    cat <<EOF
${BLUE}Spring Boot Repo - Development Helper${NC}

USAGE:
    ./scripts/dev.sh [COMMAND] [OPTIONS]

COMMANDS:
    help                      Show this help message
    setup                     Initial development environment setup
    build                     Build all projects
    build-azure              Build azure-java-platform only
    build-farm               Build farm-workers-api only
    build-academy            Build spring-academy-intro only
    test                     Run all tests
    test-azure               Test azure-java-platform only
    test-farm                Test farm-workers-api only
    test-academy             Test spring-academy-intro only
    clean                    Clean all build artifacts
    run-azure                Run azure-java-platform web API
    run-farm                 Run farm-workers-api
    run-academy              Run spring-academy-intro
    docker-up                Start Docker Compose services
    docker-down              Stop Docker Compose services
    format                   Format all code
    coverage                 Generate coverage reports
    lint                     Run code linting
    version-check            Verify tool versions
    pre-commit-install       Install pre-commit hooks
    pre-commit-run           Run pre-commit checks

EXAMPLES:
    ./scripts/dev.sh setup
    ./scripts/dev.sh build
    ./scripts/dev.sh test
    ./scripts/dev.sh run-farm

EOF
}

setup() {
    log_info "Setting up development environment..."

    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java not found. Please install Java 21 LTS"
        return 1
    fi

    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven not found. Please install Maven 3.9.6"
        return 1
    fi

    # Check Git
    if ! command -v git &> /dev/null; then
        log_error "Git not found. Please install Git"
        return 1
    fi

    log_success "All prerequisites found"

    # Install pre-commit hooks
    if command -v pre-commit &> /dev/null; then
        pre_commit_install
    else
        log_warning "pre-commit not installed. Run: pip install pre-commit && pre-commit install"
    fi

    # Configure git
    log_info "Configuring git hooks..."
    git config core.hooksPath .githooks || true

    log_success "Setup complete!"
}

build() {
    log_info "Building all projects..."
    build_azure
    build_farm
    build_academy
    log_success "All projects built successfully"
}

build_azure() {
    log_info "Building azure-java-platform..."
    cd "$AZURE_PLATFORM"
    mvn clean install -q
    cd - > /dev/null
    log_success "azure-java-platform built"
}

build_farm() {
    log_info "Building farm-workers-api..."
    cd "$FARM_WORKERS"
    mvn clean package -q
    cd - > /dev/null
    log_success "farm-workers-api built"
}

build_academy() {
    log_info "Building spring-academy-intro..."
    cd "$SPRING_ACADEMY"
    ./gradlew build -q
    cd - > /dev/null
    log_success "spring-academy-intro built"
}

test() {
    log_info "Running all tests..."
    cd "$AZURE_PLATFORM"
    mvn test -q
    cd "$FARM_WORKERS"
    mvn test -q
    cd "$SPRING_ACADEMY"
    ./gradlew test -q
    log_success "All tests passed"
}

test_azure() {
    log_info "Testing azure-java-platform..."
    cd "$AZURE_PLATFORM"
    mvn test
}

test_farm() {
    log_info "Testing farm-workers-api..."
    cd "$FARM_WORKERS"
    mvn test
}

test_academy() {
    log_info "Testing spring-academy-intro..."
    cd "$SPRING_ACADEMY"
    ./gradlew test
}

clean() {
    log_info "Cleaning build artifacts..."
    cd "$AZURE_PLATFORM"
    mvn clean -q
    cd "$FARM_WORKERS"
    mvn clean -q
    cd "$SPRING_ACADEMY"
    ./gradlew clean -q
    log_success "Clean complete"
}

run_azure() {
    log_info "Starting azure-java-platform (http://localhost:8080)..."
    cd "$AZURE_PLATFORM"
    mvn -pl webapi spring-boot:run
}

run_farm() {
    log_info "Starting farm-workers-api (http://localhost:8080)..."
    cd "$FARM_WORKERS"
    mvn spring-boot:run
}

run_academy() {
    log_info "Starting spring-academy-intro (http://localhost:8080)..."
    cd "$SPRING_ACADEMY"
    ./gradlew bootRun
}

docker_up() {
    log_info "Starting Docker Compose services..."
    cd "$AZURE_PLATFORM"
    docker compose -f local/docker-compose.yml up -d
    log_success "Docker services started"
}

docker_down() {
    log_info "Stopping Docker Compose services..."
    cd "$AZURE_PLATFORM"
    docker compose -f local/docker-compose.yml down
    log_success "Docker services stopped"
}

format() {
    log_info "Formatting code..."
    cd "$AZURE_PLATFORM"
    mvn spotless:apply -q
    cd "$FARM_WORKERS"
    mvn spotless:apply -q
    log_success "Code formatted"
}

coverage() {
    log_info "Generating coverage reports..."
    cd "$AZURE_PLATFORM"
    mvn test jacoco:report -q
    log_info "Coverage report: $AZURE_PLATFORM/target/site/jacoco/index.html"
    cd "$FARM_WORKERS"
    mvn test jacoco:report -q
    log_info "Coverage report: $FARM_WORKERS/target/site/jacoco/index.html"
}

lint() {
    log_info "Running code linting..."
    cd "$AZURE_PLATFORM"
    mvn checkstyle:check -q
    cd "$FARM_WORKERS"
    mvn checkstyle:check -q
    log_success "Linting complete"
}

version_check() {
    log_info "Checking tool versions..."

    java -version
    echo
    mvn --version
    echo
    if command -v gradle &> /dev/null; then
        gradle --version
    else
        cd "$SPRING_ACADEMY"
        ./gradlew --version
    fi
    echo
    git --version
}

pre_commit_install() {
    if ! command -v pre-commit &> /dev/null; then
        log_error "pre-commit not installed. Install with: pip install pre-commit"
        return 1
    fi
    log_info "Installing pre-commit hooks..."
    pre-commit install
    pre-commit install --hook-type commit-msg
    log_success "Pre-commit hooks installed"
}

pre_commit_run() {
    if ! command -v pre-commit &> /dev/null; then
        log_error "pre-commit not installed"
        return 1
    fi
    log_info "Running pre-commit checks..."
    pre-commit run --all-files
}

# Main
COMMAND="${1:-help}"

case "$COMMAND" in
    help) help ;;
    setup) setup ;;
    build) build ;;
    build-azure) build_azure ;;
    build-farm) build_farm ;;
    build-academy) build_academy ;;
    test) test ;;
    test-azure) test_azure ;;
    test-farm) test_farm ;;
    test-academy) test_academy ;;
    clean) clean ;;
    run-azure) run_azure ;;
    run-farm) run_farm ;;
    run-academy) run_academy ;;
    docker-up) docker_up ;;
    docker-down) docker_down ;;
    format) format ;;
    coverage) coverage ;;
    lint) lint ;;
    version-check) version_check ;;
    pre-commit-install) pre_commit_install ;;
    pre-commit-run) pre_commit_run ;;
    *)
        log_error "Unknown command: $COMMAND"
        help
        exit 1
        ;;
esac


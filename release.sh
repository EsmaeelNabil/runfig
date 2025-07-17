#!/bin/bash

# Runfig Release Script
# Usage: ./release.sh [local|central] [version]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
RELEASE_TYPE="local"
VERSION=""

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [local|central] [version]"
    echo ""
    echo "Options:"
    echo "  local     - Publish to local Maven repository (~/.m2/repository)"
    echo "  central   - Publish to Maven Central"
    echo "  version   - Optional version to update (e.g., 0.0.4)"
    echo ""
    echo "Examples:"
    echo "  $0 local              # Publish current version locally"
    echo "  $0 central            # Publish current version to Maven Central"
    echo "  $0 local 0.0.4        # Update to version 0.0.4 and publish locally"
    echo "  $0 central 0.0.4      # Update to version 0.0.4 and publish to Maven Central"
}

# Function to update version in build files
update_version() {
    local new_version=$1
    print_status "Updating version to $new_version..."
    
    # Update Android module version
    sed -i.bak "s/version = \".*\"/version = \"$new_version\"/" runfig-android/build.gradle.kts
    
    # Update Gradle plugin version
    sed -i.bak "s/version = \".*\"/version = \"$new_version\"/" runfig-gradle-plugin/build.gradle.kts
    
    # Remove backup files
    rm -f runfig-android/build.gradle.kts.bak
    rm -f runfig-gradle-plugin/build.gradle.kts.bak
    
    print_success "Version updated to $new_version"
}

# Function to get current version
get_current_version() {
    grep 'version = ' runfig-android/build.gradle.kts | head -1 | sed 's/.*version = "\(.*\)".*/\1/'
}

# Function to run pre-release checks
run_checks() {
    print_status "Running pre-release checks..."
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository!"
        exit 1
    fi
    
    # Check for uncommitted changes
    if [[ -n $(git status --porcelain) ]]; then
        print_warning "You have uncommitted changes. Consider committing them first."
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    # Check if gradle.properties exists and has credentials
    if [[ "$RELEASE_TYPE" == "central" ]]; then
        if [[ ! -f ~/.gradle/gradle.properties ]]; then
            print_error "Global ~/.gradle/gradle.properties not found!"
            print_error "Please ensure you have Maven Central credentials configured."
            exit 1
        fi
        
        if ! grep -q "mavenCentralUsername" ~/.gradle/gradle.properties; then
            print_error "Maven Central credentials not found in ~/.gradle/gradle.properties"
            exit 1
        fi
    fi
    
    print_success "Pre-release checks passed"
}

# Function to build and test
build_and_test() {
    print_status "Building and testing project..."
    
    # Clean build
    ./gradlew clean
    
    # Build both modules
    ./gradlew :runfig-android:build :runfig-gradle-plugin:build
    
    # Run tests
    ./gradlew test
    
    print_success "Build and tests completed successfully"
}

# Function to publish locally
publish_local() {
    print_status "Publishing to local Maven repository..."
    
    # Publish to local repository
    ./gradlew publishToMavenLocal
    
    print_success "Published to local Maven repository (~/.m2/repository)"
    print_status "You can now use the library in other projects with:"
    echo "  implementation 'dev.supersam.runfig:runfig-android:$(get_current_version)'"
    echo "  id 'dev.supersam.runfig.gradle' version '$(get_current_version)'"
}

# Function to publish to Maven Central
publish_central() {
    print_status "Publishing to Maven Central..."
    
    # Publish to Maven Central
    ./gradlew publishAndReleaseToMavenCentral
    
    print_success "Published to Maven Central!"
    print_status "It may take a few minutes to appear in search results."
    print_status "Check status at: https://central.sonatype.com/namespace/dev.supersam.runfig"
}

# Function to create git tag
create_git_tag() {
    if [[ -n "$VERSION" ]]; then
        local tag_name="v$VERSION"
        print_status "Creating git tag: $tag_name"
        
        git add .
        git commit -m "Release version $VERSION" || true
        git tag -a "$tag_name" -m "Release version $VERSION"
        
        print_success "Git tag created: $tag_name"
        print_status "Push with: git push origin main --tags"
    fi
}

# Main script logic
main() {
    # Parse arguments
    if [[ $# -gt 0 ]]; then
        case $1 in
            local|central)
                RELEASE_TYPE=$1
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                print_error "Invalid release type: $1"
                show_usage
                exit 1
                ;;
        esac
    fi
    
    if [[ $# -gt 1 ]]; then
        VERSION=$2
        if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            print_error "Invalid version format: $VERSION (expected: x.y.z)"
            exit 1
        fi
    fi
    
    # Show current configuration
    print_status "Release Configuration:"
    echo "  Type: $RELEASE_TYPE"
    echo "  Current version: $(get_current_version)"
    if [[ -n "$VERSION" ]]; then
        echo "  New version: $VERSION"
    fi
    echo ""
    
    # Confirm before proceeding
    read -p "Continue with release? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Release cancelled."
        exit 0
    fi
    
    # Update version if specified
    if [[ -n "$VERSION" ]]; then
        update_version "$VERSION"
    fi
    
    # Run checks
    run_checks
    
    # Build and test
    build_and_test
    
    # Publish based on type
    case $RELEASE_TYPE in
        local)
            publish_local
            ;;
        central)
            publish_central
            create_git_tag
            ;;
    esac
    
    print_success "Release completed successfully! 🎉"
}

# Run main function
main "$@"
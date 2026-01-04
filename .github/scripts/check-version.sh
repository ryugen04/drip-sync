#!/bin/bash
set -e

# バージョンチェックスクリプト
# PRでモジュールに変更がある場合、versionCodeがインクリメントされているかチェック

BASE_BRANCH="${1:-main}"

# versionCodeを抽出する関数
extract_version_code() {
    local file="$1"
    grep -E "versionCode\s*=" "$file" | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/'
}

# 変更されたファイルを取得
get_changed_files() {
    git diff --name-only "origin/${BASE_BRANCH}...HEAD"
}

# モジュールに変更があるかチェック
has_changes_in() {
    local module="$1"
    get_changed_files | grep -q "^${module}/"
}

# メイン処理
main() {
    echo "Checking version increments..."
    echo "Base branch: ${BASE_BRANCH}"
    echo ""

    # 変更されたファイル一覧
    echo "Changed files:"
    get_changed_files | head -20
    echo ""

    local check_mobile=false
    local check_wear=false

    # sharedに変更がある場合は両方チェック
    if has_changes_in "shared"; then
        echo "shared/ has changes -> checking both mobile and wear"
        check_mobile=true
        check_wear=true
    fi

    # 各モジュールの変更をチェック
    if has_changes_in "mobile"; then
        echo "mobile/ has changes"
        check_mobile=true
    fi

    if has_changes_in "wear"; then
        echo "wear/ has changes"
        check_wear=true
    fi

    # チェック対象がない場合
    if ! $check_mobile && ! $check_wear; then
        echo "No mobile/wear/shared changes detected. Skipping version check."
        exit 0
    fi

    echo ""
    local failed=false

    # mobileのバージョンチェック
    if $check_mobile; then
        echo "Checking mobile version..."
        local current_version
        local base_version

        current_version=$(extract_version_code "mobile/build.gradle.kts")
        base_version=$(git show "origin/${BASE_BRANCH}:mobile/build.gradle.kts" 2>/dev/null | grep -E "versionCode\s*=" | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/' || echo "0")

        echo "  Base version: ${base_version}"
        echo "  Current version: ${current_version}"

        if [ "$current_version" -le "$base_version" ]; then
            echo "  ERROR: mobile versionCode must be incremented (current: ${current_version}, base: ${base_version})"
            failed=true
        else
            echo "  OK: mobile versionCode incremented"
        fi
    fi

    # wearのバージョンチェック
    if $check_wear; then
        echo "Checking wear version..."
        local current_version
        local base_version

        current_version=$(extract_version_code "wear/build.gradle.kts")
        base_version=$(git show "origin/${BASE_BRANCH}:wear/build.gradle.kts" 2>/dev/null | grep -E "versionCode\s*=" | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/' || echo "0")

        echo "  Base version: ${base_version}"
        echo "  Current version: ${current_version}"

        if [ "$current_version" -le "$base_version" ]; then
            echo "  ERROR: wear versionCode must be incremented (current: ${current_version}, base: ${base_version})"
            failed=true
        else
            echo "  OK: wear versionCode incremented"
        fi
    fi

    echo ""
    if $failed; then
        echo "Version check FAILED"
        echo "Please increment versionCode in the affected module(s) build.gradle.kts"
        exit 1
    else
        echo "Version check PASSED"
        exit 0
    fi
}

main

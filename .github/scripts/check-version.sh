#!/bin/bash
set -e

# バージョンチェックスクリプト
# PRでモジュールに変更がある場合、versionCodeがインクリメントされているかチェック
# 同一applicationIdのため、versionCodeは全モジュール・全履歴でユニークである必要がある

BASE_BRANCH="${1:-main}"

# versionCodeを抽出する関数
extract_version_code() {
    local file="$1"
    grep -E "versionCode\s*=" "$file" | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/'
}

# ベースブランチのversionCodeを抽出する関数
extract_base_version_code() {
    local file="$1"
    git show "origin/${BASE_BRANCH}:${file}" 2>/dev/null | grep -E "versionCode\s*=" | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/' || echo "0"
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

    # 現在のversionCodeを取得
    local current_mobile_version
    local current_wear_version
    current_mobile_version=$(extract_version_code "mobile/build.gradle.kts")
    current_wear_version=$(extract_version_code "wear/build.gradle.kts")

    # ベースブランチのversionCodeを取得
    local base_mobile_version
    local base_wear_version
    base_mobile_version=$(extract_base_version_code "mobile/build.gradle.kts")
    base_wear_version=$(extract_base_version_code "wear/build.gradle.kts")

    # 過去の最大versionCodeを算出（mobile/wearのうち大きい方）
    local max_base_version
    if [ "$base_mobile_version" -gt "$base_wear_version" ]; then
        max_base_version=$base_mobile_version
    else
        max_base_version=$base_wear_version
    fi

    echo "Base branch versions:"
    echo "  mobile: ${base_mobile_version}"
    echo "  wear: ${base_wear_version}"
    echo "  max: ${max_base_version}"
    echo ""
    echo "Current versions:"
    echo "  mobile: ${current_mobile_version}"
    echo "  wear: ${current_wear_version}"
    echo ""

    local failed=false

    # mobileのバージョンチェック（過去最大より大きいか）
    if $check_mobile; then
        echo "Checking mobile version..."
        if [ "$current_mobile_version" -le "$max_base_version" ]; then
            echo "  ERROR: mobile versionCode (${current_mobile_version}) must be greater than max base version (${max_base_version})"
            failed=true
        else
            echo "  OK: mobile versionCode > max base version"
        fi
    fi

    # wearのバージョンチェック（過去最大より大きいか）
    if $check_wear; then
        echo "Checking wear version..."
        if [ "$current_wear_version" -le "$max_base_version" ]; then
            echo "  ERROR: wear versionCode (${current_wear_version}) must be greater than max base version (${max_base_version})"
            failed=true
        else
            echo "  OK: wear versionCode > max base version"
        fi
    fi

    # mobile/wear間の重複チェック
    echo "Checking version uniqueness..."
    if [ "$current_mobile_version" -eq "$current_wear_version" ]; then
        echo "  ERROR: mobile and wear have the same versionCode (${current_mobile_version})"
        failed=true
    else
        echo "  OK: mobile and wear have different versionCodes"
    fi

    echo ""
    if $failed; then
        echo "Version check FAILED"
        echo ""
        echo "Rules:"
        echo "  1. versionCode must be greater than max of previous mobile/wear versions"
        echo "  2. mobile and wear must have different versionCodes"
        exit 1
    else
        echo "Version check PASSED"
        exit 0
    fi
}

main

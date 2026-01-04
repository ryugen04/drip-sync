# DripSync プロジェクト設定

## プロジェクト概要

Wear OSとAndroidスマートフォンを連携した水分摂取記録アプリ。
ウォッチから2タップで水分記録が完了する手軽さを実現。

## 技術スタック

| カテゴリ | 技術 | バージョン |
|----------|------|-----------|
| 言語 | Kotlin | 2.0.21 |
| UI（モバイル） | Jetpack Compose | BOM 2024.12.01 |
| UI（Wear OS） | Compose for Wear OS + Horologist | 1.4.0 / 0.6.22 |
| タイル | Wear Tiles API | 1.4.1 |
| データ同期 | Wear Data Layer API | 18.2.0 |
| 健康データ連携 | Health Connect API | 1.1.0-alpha10 |
| ローカルDB | Room | 2.6.1 |
| DI | Hilt | 2.53.1 |
| 設定保存 | DataStore Preferences | 1.1.1 |
| 非同期処理 | Kotlin Coroutines + Flow | 1.9.0 |
| バックグラウンド処理 | WorkManager | 2.10.0 |

## ディレクトリ構成

```
drip-sync/
├── mobile/                    # スマートフォンアプリ
│   └── src/main/kotlin/com/dripsync/mobile/
│       ├── ui/                # Compose UI（画面ごとにディレクトリ）
│       ├── sync/              # Data Layer通信
│       └── health/            # Health Connect連携
│
├── wear/                      # Wear OSアプリ
│   └── src/main/kotlin/com/dripsync/wear/
│       ├── ui/                # Compose for Wear OS UI
│       │   ├── home/          # メイン画面
│       │   ├── history/       # 履歴画面
│       │   ├── settings/      # 設定画面
│       │   ├── stats/         # 統計画面
│       │   └── theme/         # テーマ定義
│       ├── tile/              # タイル実装（HydrationTileService）
│       ├── complication/      # コンプリケーション
│       └── sync/              # Data Layer通信
│
└── shared/                    # 共通モジュール
    └── src/main/kotlin/com/dripsync/shared/
        ├── domain/model/      # ドメインモデル（Hydration）
        ├── data/
        │   ├── local/         # Room DB, DAO
        │   └── repository/    # Repository実装
        ├── di/                # Hiltモジュール
        ├── sync/              # Data Layer共通定義（パス）
        └── util/              # ユーティリティ
```

## コーディング規約

### アーキテクチャ

- MVVM + Repository パターン
- UI層: Compose / ViewModel / UiState
- Data層: Repository → DAO → Room Database
- 同期: Data Layer API（Watch ↔ Mobile双方向）

### Kotlin

- dotfiles の `skills/rules/languages/kotlin.md` を参照
- `!!` 演算子は原則禁止（使用する場合はコメントで理由を明記）
- `var` より `val` を優先
- コルーチンスコープは `viewModelScope` / `lifecycleScope` を使用
- `GlobalScope` は使用禁止

### Compose

- State hoisting を徹底
- ViewModel は collectAsStateWithLifecycle() で状態を収集
- Composable 関数名は PascalCase
- Preview用のデータは PreviewParameterProvider を使用

### Data Layer 同期

- パス定義は `shared/sync/DataLayerPaths` に集約
- MessageClient / DataClient の使い分け:
  - MessageClient: 一時的なアクション（記録追加など）
  - DataClient: 永続的な状態同期（設定、プリセット）

### テスト

- ViewModel: JUnit + MockK + Turbine（Flow テスト）
- Repository: JUnit + MockK
- UI: Compose Testing（必要時）

## Git運用

### ブランチ戦略

- **mainブランチへの直接プッシュは禁止**
- 機能追加・修正は必ずfeatureブランチを作成
- MR（マージリクエスト）を作成し、approveを得てからマージ

### コミットメッセージ

**Conventional Commits形式を厳守（1行のみ）**

```
<type>: <description>
```

- `feat`: 新機能
- `fix`: バグ修正
- `refactor`: リファクタリング
- `docs`: ドキュメント
- `test`: テスト
- `chore`: その他

**禁止事項:**
- 2行目以降の記載（本文、フッター）
- `Co-Authored-By` などの署名
- 絵文字の使用

```bash
# OK
git commit -m "feat: 水分記録のプリセット機能を追加"

# NG（2行目以降あり）
git commit -m "feat: 水分記録のプリセット機能を追加

詳細な説明..."

# NG（Co-Authored-By あり）
git commit -m "feat: 水分記録のプリセット機能を追加

Co-Authored-By: ..."
```

## バージョン管理

### PR作成時は必ずバージョンを上げる

mobile と wear は別々のモジュールだが、**versionCode はプロジェクト全体で一意な通し番号を使用する。**

| モジュール | ファイル | 現在の値 |
|-----------|---------|---------|
| mobile | `mobile/build.gradle.kts` | versionCode: 15, versionName: "1.0.5" |
| wear | `wear/build.gradle.kts` | versionCode: 16, versionName: "1.0.5" |

**重要なルール:**

1. **versionCode の採番方法**
   - **プロジェクト全体で一意な通し番号を使用** (Play Store の要件)
   - 現在の最大値を確認し、それより大きい番号を割り当てる
   - mobile と wear で別々にインクリメントしない
   - 例: 現在の最大が 16 の場合、次は mobile=17 または wear=17

2. **変更があったモジュールのバージョンを上げる**
   - mobile のみ変更: mobile の versionCode だけインクリメント
   - wear のみ変更: wear の versionCode だけインクリメント
   - 両方変更 (shared含む): 両方の versionCode をインクリメント

3. **versionName**
   - セマンティックバージョニング（MAJOR.MINOR.PATCH）
   - 通常は mobile と wear で同じバージョンを使用

**例: 次回 wear のみ変更する場合**
```kotlin
// wear/build.gradle.kts
android {
    defaultConfig {
        versionCode = 17  // 16 → 17 (全体の通し番号)
        versionName = "1.0.6"  // パッチバージョンアップ
    }
}
```

**例: 次回 mobile と wear 両方を変更する場合**
```kotlin
// mobile/build.gradle.kts
versionCode = 17  // 15 → 17

// wear/build.gradle.kts
versionCode = 18  // 16 → 18
```

## ビルド・実行

```bash
# Wear OSアプリをビルド
./gradlew :wear:assembleDebug

# モバイルアプリをビルド
./gradlew :mobile:assembleDebug

# 全モジュールテスト
./gradlew test

# リリースビルド
./gradlew assembleRelease
```

## よく触るファイル

| 目的 | ファイル |
|------|---------|
| 水分記録モデル | `shared/.../domain/model/Hydration.kt` |
| データベース | `shared/.../data/local/DripSyncDatabase.kt` |
| リポジトリ | `shared/.../data/repository/HydrationRepositoryImpl.kt` |
| Wearホーム画面 | `wear/.../ui/home/HomeScreen.kt` |
| タイル実装 | `wear/.../tile/HydrationTileService.kt` |
| 同期パス定義 | `shared/.../sync/DataLayerPaths.kt` |

## 注意事項

- Wear OSのバッテリー消費に配慮（同期頻度、バックグラウンド処理）
- タイルは更新頻度が制限されるため、リアルタイム性より効率を優先
- Health Connectは権限周りが複雑なため、権限チェックを忘れずに

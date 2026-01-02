# Firebase App Distribution デプロイ設定ガイド

## 概要

このガイドでは、GitHub Actions を使用して Firebase App Distribution に自動デプロイする環境を構築する手順を説明します。

## 前提条件

- GitHub リポジトリへのアクセス権
- Firebase プロジェクトを作成できる Google アカウント

---

## 1. Firebase プロジェクトの設定

### 1.1 Firebase プロジェクト作成

1. [Firebase Console](https://console.firebase.google.com/) にアクセス
2. 「プロジェクトを追加」をクリック
3. プロジェクト名: `dripsync` (または任意の名前)
4. Google Analytics は任意（無効でもOK）

### 1.2 Android アプリの登録

**Mobile アプリ:**
1. 「Android アプリを追加」をクリック
2. パッケージ名: `com.dripsync.mobile`
3. アプリのニックネーム: `DripSync Mobile`
4. `google-services.json` はダウンロード不要（App Distribution のみ使用）

**Wear OS アプリ:**
1. 再度「アプリを追加」→「Android」
2. パッケージ名: `com.dripsync.wear`
3. アプリのニックネーム: `DripSync Wear`

### 1.3 App Distribution の有効化

1. 左メニューから「リリースとモニタリング」→「App Distribution」を選択
2. 「使ってみる」をクリック

### 1.4 テスターグループの作成

1. App Distribution 画面で「テスターとグループ」タブを開く
2. 「グループを追加」をクリック
3. グループ名: `testers`
4. テスターのメールアドレスを追加

---

## 2. サービスアカウントの設定

### 2.1 サービスアカウント作成

1. [Google Cloud Console](https://console.cloud.google.com/) にアクセス
2. Firebase と同じプロジェクトを選択
3. 「IAM と管理」→「サービスアカウント」を開く
4. 「サービスアカウントを作成」をクリック
5. 名前: `github-actions-deployer`
6. 「作成して続行」をクリック

### 2.2 権限の付与

以下のロールを付与:
- `Firebase App Distribution 管理者`

### 2.3 キーの作成

1. 作成したサービスアカウントをクリック
2. 「キー」タブを開く
3. 「鍵を追加」→「新しい鍵を作成」
4. JSON 形式を選択
5. ダウンロードされた JSON ファイルの内容を保存

---

## 3. GitHub Secrets の設定

リポジトリの Settings → Secrets and variables → Actions で以下を設定:

### 3.1 キーストア関連

| Secret名 | 値 |
|----------|-----|
| `KEYSTORE_BASE64` | `base64 -w 0 release.keystore` の出力 |
| `KEYSTORE_PASSWORD` | `dripsync123` |
| `KEY_ALIAS` | `dripsync` |
| `KEY_PASSWORD` | `dripsync123` |

**KEYSTORE_BASE64 の取得方法:**
```bash
base64 -w 0 release.keystore
```

### 3.2 Firebase 関連

| Secret名 | 値 |
|----------|-----|
| `FIREBASE_MOBILE_APP_ID` | Firebase Console → プロジェクト設定 → Mobile アプリの「アプリ ID」 |
| `FIREBASE_WEAR_APP_ID` | Firebase Console → プロジェクト設定 → Wear アプリの「アプリ ID」 |
| `FIREBASE_SERVICE_ACCOUNT` | サービスアカウント JSON ファイルの内容全体 |

**アプリ ID の確認方法:**
1. Firebase Console → プロジェクト設定（歯車アイコン）
2. 「マイアプリ」セクションで各アプリの「アプリ ID」を確認
3. 形式: `1:123456789:android:abcdef123456`

---

## 4. デプロイの実行

### 自動デプロイ

以下のタイミングで自動的にデプロイが実行されます:
- `main` ブランチへの push
- `develop` ブランチへの push

### 手動デプロイ

1. GitHub リポジトリの「Actions」タブを開く
2. 左メニューから「Build and Deploy to Firebase」を選択
3. 「Run workflow」をクリック
4. ブランチと flavor を選択して実行

---

## 5. 実機へのインストール

### テスターとして招待される場合

1. Firebase から招待メールが届く
2. メール内のリンクをクリック
3. 「Firebase App Tester」アプリをインストール
4. アプリ内から DripSync をインストール

### Wear OS へのインストール

1. スマートフォンに「Firebase App Tester」をインストール
2. スマートフォンとウォッチをペアリング
3. Firebase App Tester から Wear OS 版を選択
4. ウォッチに自動的にインストールされる

---

## トラブルシューティング

### ビルドが失敗する場合

- GitHub Secrets が正しく設定されているか確認
- `KEYSTORE_BASE64` が正しくエンコードされているか確認

### Firebase デプロイが失敗する場合

- サービスアカウントに正しい権限があるか確認
- アプリ ID が正しいか確認（パッケージ名ではない）

### Wear OS にインストールできない場合

- スマートフォンとウォッチが同じ Google アカウントでログインしているか確認
- ウォッチが Wi-Fi に接続されているか確認

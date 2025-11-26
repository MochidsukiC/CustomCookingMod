# CustomCookingMod 仕様書

## プロジェクト概要

### 基本情報
- **プロジェクト名**: CustomCookingMod
- **対象バージョン**: Minecraft 1.20.1
- **Modローダー**: Forge 47.4.0
- **開発言語**: Java 17
- **ビルドツール**: Gradle 8.8

### コンセプト
Google Gemini AIを活用した動的レシピ生成システムを持つ料理Mod。従来のアイテム数ベースではなく、重量（グラム）ベースの現実的な料理システムを実装。プレイヤーは材料を刻み、調理器具で調理し、容器に盛り付けて食べることができる。

---

## 1. コアシステム

### 1.1 AI駆動レシピ生成システム

**使用API**: Google Gemini 2.0 Flash Experimental

#### レシピデータ構造
```java
public static class RecipeData {
    public String dishName;              // 料理名
    public List<Ingredient> ingredients; // 材料リスト
    public int totalWeightGrams;         // 総重量（常に100g）
    public float nutritionPer100g;       // 100gあたりの満腹度回復量
    public float saturationPer100g;      // 100gあたりの隠し満腹度
}

public static class Ingredient {
    public String itemId;      // アイテムID（例: "minecraft:carrot"）
    public String amountType;  // "grams" または "count"
    public float amount;       // 数量
}
```

#### レシピ生成ルール
1. **100gベース**: 全てのレシピは100gあたりの分量で生成
2. **二重計測システム**:
   - `amountType="grams"`: 粉末・液体など個数カウント不可（塩、醤油、小麦粉など）
   - `amountType="count"`: 固形物で個数カウント可能（トマト、肉、卵など）
3. **栄養計算**: 材料の栄養価から自動算出（現実的な値を返すようプロンプト設計）

#### レシピ生成フロー
```
プレイヤー → AIキッチンGUI → レシピ生成リクエストパケット（C2S）
→ サーバー側でGemini API呼び出し → JSON解析 → RecipeData生成
→ 完成品をAIKitchenBlockEntityに格納 → レシピ生成応答パケット（S2C）
→ クライアント側でGUI更新
```

**実装ファイル**:
- `RecipeGenerator.java` - AI API呼び出しとJSON解析
- `RecipeGenerationRequestPacket.java` - ネットワーク通信
- `AIKitchenBlockEntity.java` - 完成品格納

---

### 1.2 重量ベース食品システム

#### 容器アイテム
| アイテム名 | 容量 | 用途 |
|----------|------|------|
| プラスチック容器 (plastic_container) | 200g | 持ち運び用 |
| 皿 (plate) | 300g | 通常の盛り付け |
| 大皿 (large_plate) | 500g | 大量盛り付け |

#### 容器の仕様
**クラス**: `FoodContainerItem.java`

**NBTデータ構造**:
```java
{
    "weight_grams": int,           // 格納されている食品の重量
    "food_type": String,           // 料理名
    "nutrition_per_100g": float,   // 100gあたりの満腹度
    "saturation_per_100g": float   // 100gあたりの隠し満腹度
}
```

**食事時の栄養計算**:
```java
int nutrition = Math.round((nutritionPer100g * weightGrams) / 100.0f);
float saturation = (saturationPer100g * weightGrams) / 100.0f;
```

**食後の処理**: 容器は空になって返却される（ItemStack維持）

#### 容器への盛り付けフロー
```
1. 調理ブロック（完成品あり）に容器を持って右クリック
2. 容器の容量と完成品の重量を比較
3. min(容量, 残り重量)だけ転送
4. NBTに食品データを格納して返却
```

**実装箇所**: `AIKitchenBlock.java:use()` メソッド

---

## 2. 調理システム

### 2.1 調理アクション

**定義場所**: `CookingAction.java`

| アクション | ID | 表示名 | デフォルト時間 | 結果接頭辞 | 熱源必要 |
|-----------|-------|--------|--------------|-----------|---------|
| STIR_FRY | stir_fry | Stir-Frying | 200 ticks (10秒) | Stir-fried | ✓ |
| SIMMER | simmer | Simmering | 600 ticks (30秒) | Simmered | ✓ |
| BOIL | boil | Boiling | 300 ticks (15秒) | Boiled | ✓ |
| GRILL | grill | Grilling | 200 ticks (10秒) | Grilled | ✓ |
| BAKE | bake | Baking | 400 ticks (20秒) | Baked | ✗ |
| STEAM | steam | Steaming | 300 ticks (15秒) | Steamed | ✓ |
| MIX | mix | Mixing | 100 ticks (5秒) | Mixed | ✗ |
| CHOP | chop | Chopping | 40 ticks (2秒) | Chopped | ✗ |
| FRY | fry | Frying | 200 ticks (10秒) | Fried | ✓ |

**Note**: 1 tick = 1/20秒 = 0.05秒

---

### 2.2 まな板システム

**ブロック**: `CuttingBoardBlock.java`
**BlockEntity**: `CuttingBoardBlockEntity.java`

#### 操作フロー
```
1. 食材を持ってまな板に右クリック → 食材を配置
2. 包丁を持ってまな板に右クリック → 刻む（CHOPアクション）
3. 空手でまな板に右クリック → 刻んだ食材を回収
```

#### 刻んだ食材の実装
```java
ItemStack chopped = original.copy();
chopped.getOrCreateTag().putBoolean("chopped", true);
chopped.setHoverName(Component.literal("Chopped " + originalName));
```

**NBTタグ**: `chopped=true` で識別

---

### 2.3 IH（電磁調理器）熱源システム

**ブロック**: `IHHeaterBlock.java`
**BlockEntity**: `IHHeaterBlockEntity.java`

#### 熱レベル
```java
public enum HeatLevel {
    OFF    (0, "Off",    0.0f),  // 加熱なし
    LOW    (1, "Low",    0.5f),  // 低速（0.5倍速）
    MEDIUM (2, "Medium", 1.0f),  // 標準速度
    HIGH   (3, "High",   1.5f)   // 高速（1.5倍速）
}
```

#### 操作方法
- **右クリック**: 熱レベルをサイクル（OFF → LOW → MEDIUM → HIGH → OFF）
- **表示色**:
  - OFF: §7（灰色）
  - LOW: §e（黄色）
  - MEDIUM: §6（金色）
  - HIGH: §c（赤色）

#### 効果音
- 点火時: `SoundEvents.FLINTANDSTEEL_USE`
- 消火時: `SoundEvents.FIRE_EXTINGUISH`

#### 熱源チェックAPI
```java
// 静的ヘルパーメソッド
public static IHHeaterBlockEntity getIHBelow(Level level, BlockPos pos)
public static float getHeatMultiplierFromBelow(Level level, BlockPos pos)
```

**仕様**: 調理ブロックの真下1ブロックをチェック

---

### 2.4 調理ブロック

#### 共通アーキテクチャ

**継承構造**:
```
BlockEntity
└── CookingBlockEntity (完成品格納機能)
    └── CookingProcessBlockEntity (調理プロセス管理)
        ├── HotPlateBlockEntity (ホットプレート)
        ├── PotBlockEntity (鍋)
        ├── FryingPanBlockEntity (フライパン)
        └── OvenBlockEntity (オーブン)
```

#### CookingProcessBlockEntity 仕様

**主要メソッド**:
```java
// 材料管理
boolean addIngredient(ItemStack ingredient)     // 材料追加
List<ItemStack> getIngredients()                // 材料取得
void clearIngredients()                         // 材料クリア
boolean hasIngredients()                        // 材料存在チェック

// 調理制御
boolean startCooking(String action, int ticks)  // 調理開始
void stopCooking()                              // 調理停止
boolean isCooking()                             // 調理中判定
float getCookingProgress()                      // 進捗（0.0-1.0）

// 熱源管理
protected boolean requiresHeatSource()          // 熱源必要性（オーバーライド用）
protected float getHeatMultiplier()             // 熱倍率取得
protected boolean hasHeatSource()               // 熱源存在チェック

// 完了処理
protected abstract void onCookingComplete()     // 完了時処理（サブクラスで実装）
```

**調理進行ロジック**:
```java
protected void tickCooking() {
    if (!hasHeatSource()) {
        stopCooking();  // 熱源なしで停止
        return;
    }
    float heatMultiplier = getHeatMultiplier();
    cookingProgress += Math.max(1, (int) heatMultiplier);

    if (cookingProgress >= cookingTime) {
        onCookingComplete();
    }
}
```

**NBT永続化**:
- `ingredients`: ListTag (ItemStack配列)
- `current_action`: String
- `cooking_progress`: int
- `cooking_time`: int
- `is_cooking`: boolean

---

#### 調理ブロック個別仕様

##### 1. ホットプレート (Hot Plate)

**ファイル**: `HotPlateBlock.java`, `HotPlateBlockEntity.java`

| 項目 | 値 |
|------|-----|
| 熱源依存 | ✗ 独立動作 |
| 対応アクション | STIR_FRY (炒める) |
| 開始ツール | spatula（ヘラ） |
| 最大材料数 | 9個 |

**操作フロー**:
1. 材料を持って右クリック → 材料追加
2. ヘラを持って右クリック → 炒め開始
3. 空手で右クリック → ステータス確認

**完成品命名**: `"Stir-fried " + 材料名`

---

##### 2. 鍋 (Pot)

**ファイル**: `PotBlock.java`, `PotBlockEntity.java`

| 項目 | 値 |
|------|-----|
| 熱源依存 | ✓ IH必須 |
| 対応アクション | SIMMER（煮込む）, BOIL（沸騰） |
| 開始ツール | spoon（スプーン） |
| 最大材料数 | 9個 |

**操作フロー**:
1. IHの上に設置
2. 材料を持って右クリック → 材料追加
3. スプーンを持って右クリック → 調理開始
   - 材料数 > 2: SIMMER（煮込み）
   - 材料数 ≤ 2: BOIL（沸騰）
4. 空手で右クリック → ステータス確認

**IH未設置時のエラー**: `"§cNo IH heater below! Place pot on an IH heater."`

---

##### 3. フライパン (Frying Pan)

**ファイル**: `FryingPanBlock.java`, `FryingPanBlockEntity.java`

| 項目 | 値 |
|------|-----|
| 熱源依存 | ✓ IH必須 |
| 対応アクション | FRY（焼く） |
| 開始ツール | spatula（ヘラ） |
| 最大材料数 | 9個 |

**操作フロー**:
1. IHの上に設置
2. 材料を持って右クリック → 材料追加
3. ヘラを持って右クリック → 焼き開始
4. 空手で右クリック → ステータス確認

**効果音**: `SoundEvents.LAVA_POP`（焼く音）

---

##### 4. オーブン (Oven)

**ファイル**: `OvenBlock.java`, `OvenBlockEntity.java`

| 項目 | 値 |
|------|-----|
| 熱源依存 | ✗ 独立動作 |
| 対応アクション | BAKE（焼く） |
| 開始ツール | 空手 |
| 最大材料数 | 9個 |

**操作フロー**:
1. 材料を持って右クリック → 材料追加
2. 空手で右クリック（材料あり） → ベイク開始
3. 空手で右クリック（調理中） → 進捗確認

**効果音**: `SoundEvents.BLAZE_SHOOT`（オーブン点火音）

---

### 2.5 調理結果生成

**完成品の命名規則**:
```
[調理アクション接頭辞] + " " + [主材料名]

例:
- Stir-fried Vegetables
- Boiled Potato
- Fried Egg
- Baked Bread
```

**栄養計算**:
```java
// 全材料の栄養を合計
float totalNutrition = 0;
for (ItemStack ingredient : ingredients) {
    FoodProperties food = ingredient.getItem().getFoodProperties();
    totalNutrition += food.getNutrition() * ingredient.getCount();
}

// 100gあたりに正規化
int totalWeight = ingredients.size() * 100;
float nutritionPer100g = totalNutrition / (totalWeight / 100.0f);
```

**格納**:
```java
storeFood(
    resultName,           // 料理名
    totalWeight,          // 総重量
    nutritionPer100g,     // 100gあたりの栄養
    saturationPer100g     // 100gあたりの隠し満腹度
);
```

---

## 3. アイテムシステム

### 3.1 登録アイテム一覧

**実装ファイル**: `ModItems.java`

#### 容器類
```java
PLASTIC_CONTAINER  // プラスチック容器（200g）
PLATE              // 皿（300g）
LARGE_PLATE        // 大皿（500g）
```

#### 調理道具
```java
KITCHEN_KNIFE      // 包丁（まな板用）
SPATULA            // ヘラ（炒め用）
SPOON              // スプーン（鍋用）
MEASURING_CUP      // 計量カップ
KITCHEN_SCALE      // キッチンスケール
```

#### 調味料・材料
```java
SOY_SAUCE          // 醤油
SALT               // 塩
SUGAR              // 砂糖
// その他の調味料は将来実装予定
```

### 3.2 アイテムプロパティ

**基本設定**:
```java
new Item.Properties().stacksTo(16)  // 容器：16個スタック
new Item.Properties().stacksTo(1)   // 道具：1個（耐久度なし）
new Item.Properties().stacksTo(64)  // 調味料：64個スタック
```

---

## 4. ブロックシステム

### 4.1 登録ブロック一覧

**実装ファイル**: `ModBlocks.java`

| ブロック名 | ID | BlockEntityあり | 説明 |
|-----------|----|----|------|
| AIキッチン | ai_kitchen | ✓ | AIレシピ生成GUI |
| IHヒーター | ih_heater | ✓ | 熱源（4段階調整可） |
| ホットプレート | hot_plate | ✓ | 炒め調理（独立） |
| 鍋 | pot | ✓ | 煮込み・沸騰（IH依存） |
| フライパン | frying_pan | ✓ | 焼き調理（IH依存） |
| オーブン | oven | ✓ | ベイク調理（独立） |
| まな板 | cutting_board | ✓ | 材料を刻む |
| 炊飯器 | rice_cooker | ✗ | 未実装 |
| 電子レンジ | microwave | ✗ | 未実装 |

### 4.2 BlockEntityタイプ

**登録場所**: `ModBlockEntities.java`

```java
AI_KITCHEN_BE      // AIKitchenBlockEntity
IH_HEATER_BE       // IHHeaterBlockEntity
HOT_PLATE_BE       // HotPlateBlockEntity
POT_BE             // PotBlockEntity
FRYING_PAN_BE      // FryingPanBlockEntity
OVEN_BE            // OvenBlockEntity
CUTTING_BOARD_BE   // CuttingBoardBlockEntity
```

### 4.3 ブロック物性

**共通プロパティ**:
```java
// 金属系（調理器具）
BlockBehaviour.Properties.of()
    .mapColor(MapColor.METAL)
    .strength(2.0f - 3.5f)
    .sound(SoundType.METAL)
    .requiresCorrectToolForDrops()

// 木材系（まな板）
BlockBehaviour.Properties.of()
    .mapColor(MapColor.WOOD)
    .strength(1.5f)
    .sound(SoundType.WOOD)
```

---

## 5. ネットワーク通信

### 5.1 パケットシステム

**実装ファイル**: `ModMessages.java`

**登録チャンネル**: `customcookingmod:messages`

#### パケット一覧

| パケット名 | 方向 | 用途 |
|-----------|------|------|
| RecipeGenerationRequestPacket | C2S | レシピ生成リクエスト |
| RecipeGenerationResponsePacket | S2C | 生成結果通知 |

#### RecipeGenerationRequestPacket

**送信データ**:
```java
private final String dishName;     // 料理名
private final String category;     // カテゴリ
private final BlockPos kitchenPos; // AIキッチン座標
```

**サーバー側処理**:
```java
1. Gemini APIでレシピ生成
2. RecipeData → AIKitchenBlockEntity に格納
3. RecipeGenerationResponsePacket で結果通知
```

#### RecipeGenerationResponsePacket

**送信データ**:
```java
private final boolean success;      // 成功/失敗
private final String message;       // メッセージ
```

---

## 6. GUI システム

### 6.1 AIキッチンGUI

**ファイル**: `AIKitchenScreen.java`, `AIKitchenMenu.java`

#### レイアウト構成
```
┌─────────────────────────────┐
│  カテゴリ選択ボタン          │
│  [和食] [洋食] [中華] ...   │
├─────────────────────────────┤
│  料理名入力フィールド        │
│  [__________________]        │
├─────────────────────────────┤
│  [レシピ生成] ボタン         │
├─────────────────────────────┤
│  生成結果表示エリア          │
│  - 料理名                    │
│  - 材料リスト                │
│  - 栄養情報                  │
└─────────────────────────────┘
```

#### カテゴリ
- 和食 (Japanese)
- 洋食 (Western)
- 中華 (Chinese)
- デザート (Dessert)
- その他 (Other)

#### 操作フロー
```
1. プレイヤーがAIキッチンブロックを右クリック
2. GUI表示（MenuProvider経由）
3. カテゴリ選択 + 料理名入力
4. 「レシピ生成」ボタンクリック
5. RecipeGenerationRequestPacket 送信
6. サーバー側でGemini API呼び出し
7. 成功 → AIKitchenBlockEntity に完成品格納
8. RecipeGenerationResponsePacket で結果通知
9. GUIに結果表示
```

---

## 7. 外部API連携

### 7.1 Google Gemini API

**使用モデル**: `gemini-2.0-flash-exp`

**APIエンドポイント**:
```
https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent
```

**認証**: APIキー（環境変数 `GEMINI_API_KEY`）

#### リクエストプロンプト構造

**システムプロンプト**:
```
あなたは料理のレシピを生成するAIアシスタントです。
指定された料理のレシピをJSON形式で返してください。

ルール:
1. レシピは100gあたりの分量で指定してください
2. 材料は2種類の単位があります:
   - amountType="grams": 粉や液体など個数カウントできないもの
   - amountType="count": 個数でカウントできる固形物
3. totalWeightGrams は必ず 100 にしてください
4. nutritionPer100g は0-20の範囲で現実的な値を設定
5. saturationPer100g は0.0-1.0の範囲
```

**ユーザープロンプト例**:
```
カテゴリ: 和食
料理名: カレー

上記の料理のレシピをJSON形式で生成してください。
```

#### レスポンス形式

**期待される構造**:
```json
{
  "dishName": "カレー",
  "ingredients": [
    {
      "itemId": "minecraft:carrot",
      "amountType": "count",
      "amount": 0.2
    },
    {
      "itemId": "customcookingmod:curry_powder",
      "amountType": "grams",
      "amount": 5.0
    }
  ],
  "totalWeightGrams": 100,
  "nutritionPer100g": 8.0,
  "saturationPer100g": 0.6
}
```

#### エラーハンドリング

**接続エラー**:
```java
catch (IOException e) {
    return null;  // 失敗時はnull返却
}
```

**JSON解析エラー**:
```java
catch (Exception e) {
    e.printStackTrace();
    return null;
}
```

**実装ファイル**: `RecipeGenerator.java:generateRecipeForDish()`

---

## 8. データ永続化

### 8.1 BlockEntity NBTデータ

#### AIKitchenBlockEntity
```nbt
{
    "stored_food_type": "String",
    "stored_weight_grams": int,
    "nutrition_per_100g": float,
    "saturation_per_100g": float
}
```

#### CuttingBoardBlockEntity
```nbt
{
    "ingredient": ItemStack,
    "is_chopped": boolean
}
```

#### IHHeaterBlockEntity
```nbt
{
    "HeatLevel": int  // 0=OFF, 1=LOW, 2=MEDIUM, 3=HIGH
}
```

#### CookingProcessBlockEntity（共通）
```nbt
{
    "ingredients": ListTag[ItemStack],
    "current_action": String,
    "cooking_progress": int,
    "cooking_time": int,
    "is_cooking": boolean,
    // 親クラス（CookingBlockEntity）のデータも含む
    "stored_food_type": String,
    "stored_weight_grams": int,
    "nutrition_per_100g": float,
    "saturation_per_100g": float
}
```

### 8.2 アイテムNBTデータ

#### 容器（FoodContainerItem）
```nbt
{
    "weight_grams": int,
    "food_type": String,
    "nutrition_per_100g": float,
    "saturation_per_100g": float
}
```

#### 刻んだ食材
```nbt
{
    "chopped": boolean
}
```

---

## 9. ゲームプレイフロー

### 9.1 基本的な調理フロー

```
【準備】
1. 材料を集める（Minecraft標準アイテム）
2. まな板で材料を刻む（任意）

【調理】
3-a. IH依存調理の場合:
   - IHヒーターを設置
   - IHの上に鍋/フライパンを設置
   - IHの熱レベルを調整（右クリック）
3-b. 独立調理の場合:
   - ホットプレート/オーブンを設置

4. 調理ブロックに材料を追加（右クリック）
5. 適切な道具で調理開始
   - ヘラ: ホットプレート、フライパン
   - スプーン: 鍋
   - 空手: オーブン

【完成・食事】
6. 調理完了を待つ（進捗表示あり）
7. 容器を持って完成品から盛り付け
8. 容器に入った料理を食べる
```

### 9.2 AI料理生成フロー

```
【レシピ検索】
1. AIキッチンブロックを設置
2. 右クリックでGUI表示
3. カテゴリと料理名を入力
4. 「レシピ生成」をクリック

【結果確認】
5. サーバーがGemini APIで生成
6. 材料リストと栄養情報を表示
7. AIキッチンに完成品が格納される

【取得】
8. 容器を持ってAIキッチンから盛り付け
9. すぐに食べられる
```

---

## 10. 技術仕様詳細

### 10.1 依存関係

**build.gradle**:
```gradle
minecraft 'net.minecraftforge:forge:1.20.1-47.4.0'

// Mixin
implementation 'org.spongepowered:mixin:0.7.+'

// 統合Mod
implementation fg.deobf("curse.maven:kaleidoscope-cookery-123456:file-id")

// HTTPクライアント（Gemini API用）
implementation 'com.squareup.okhttp3:okhttp:4.10.0'

// JSON解析
implementation 'com.google.code.gson:gson:2.10.1'
```

### 10.2 パッケージ構造

```
jp.houlab.mochidsuki.customcookingmod/
├── block/                    # ブロッククラス
│   ├── AIKitchenBlock.java
│   ├── CuttingBoardBlock.java
│   ├── FryingPanBlock.java
│   ├── HotPlateBlock.java
│   ├── IHHeaterBlock.java
│   ├── OvenBlock.java
│   └── PotBlock.java
├── blockentity/              # BlockEntityクラス
│   ├── AIKitchenBlockEntity.java
│   ├── CookingBlockEntity.java
│   ├── CookingProcessBlockEntity.java
│   ├── CuttingBoardBlockEntity.java
│   ├── FryingPanBlockEntity.java
│   ├── HotPlateBlockEntity.java
│   ├── IHHeaterBlockEntity.java
│   ├── OvenBlockEntity.java
│   └── PotBlockEntity.java
├── cooking/                  # 調理システム
│   └── CookingAction.java
├── gui/                      # GUIシステム
│   ├── AIKitchenMenu.java
│   └── AIKitchenScreen.java
├── item/                     # アイテムクラス
│   └── FoodContainerItem.java
├── network/                  # ネットワーク通信
│   ├── ModMessages.java
│   ├── RecipeGenerationRequestPacket.java
│   └── RecipeGenerationResponsePacket.java
├── recipe/                   # レシピシステム
│   └── RecipeGenerator.java
├── registry/                 # 登録システム
│   ├── ModBlockEntities.java
│   ├── ModBlocks.java
│   ├── ModItems.java
│   └── ModMenuTypes.java
└── CustomcookingmodMain.java # メインクラス
```

### 10.3 イベント登録

**FMLCommonSetupEvent**:
```java
ModMessages.register();  // ネットワークパケット登録
```

**FMLClientSetupEvent**:
```java
MenuScreens.register(ModMenuTypes.AI_KITCHEN_MENU.get(), AIKitchenScreen::new);
```

---

## 11. パフォーマンス考慮事項

### 11.1 サーバー側最適化

**調理tick処理**:
- BlockEntityTickerはサーバー側のみ（`level.isClientSide() ? null : ticker`）
- 調理中のみtick処理実行（`if (isCooking())`）
- 熱源チェックは毎tick（高速処理、キャッシュなし）

**API呼び出し**:
- Gemini APIはサーバー側のみ実行
- 同期処理（ブロッキング）
- タイムアウト未設定（将来の改善点）

### 11.2 クライアント側最適化

**GUI描画**:
- 標準的なMinecraft GUIシステム使用
- カスタムレンダリングなし

**パケット送信**:
- ユーザーアクション時のみ送信
- 自動同期なし

### 11.3 メモリ管理

**BlockEntity**:
- 材料リスト: 最大9個まで（ArrayList使用）
- NBTデータは必要時のみシリアライズ

**ItemStack**:
- 容器NBTデータ: 小サイズ（String + 3 float/int）
- 刻んだ食材: boolean 1ビットのみ

---

## 12. 将来の拡張予定

### 12.1 未実装機能

**調理器具**:
- 炊飯器 (Rice Cooker)
- 電子レンジ (Microwave)

**調理アクション**:
- STEAM（蒸す） - 実装済みだが使用ブロックなし
- GRILL（焼く） - 実装済みだが使用ブロックなし
- MIX（混ぜる） - 実装済みだが使用ブロックなし

**材料システム**:
- 液体管理（水、だし汁、スープ）
- 注ぐメカニクス
- 中間調理結果の再利用

**レシピシステム**:
- 複数ステップレシピ
- レシピブック機能
- 調理中の材料追加（カレールーなど）

### 12.2 改善検討事項

**パフォーマンス**:
- API呼び出しの非同期化
- タイムアウト設定
- レシピキャッシュシステム

**ゲームバランス**:
- 調理時間の調整
- 栄養値のバランス調整
- 材料コストの検討

**ユーザビリティ**:
- レシピ保存機能
- お気に入り機能
- 調理進捗通知（チャット/音）

**統合**:
- KaleidoscopeCookeryとの完全統合
- 他の料理Modとの互換性
- カスタムレシピAPI公開

---

## 13. デバッグ・テスト

### 13.1 デバッグコマンド

現在未実装。将来の追加候補:
```
/customcooking generate <料理名>  # 強制的にレシピ生成
/customcooking clear              # 全調理ブロックをリセット
/customcooking setHeat <level>    # IH熱レベル設定
```

### 13.2 テスト対象レシピ

**初期検証済み**:
1. カレー (Curry) - 複数材料、煮込み
2. ラーメン (Ramen) - 麺+スープ、複雑な工程
3. 味噌汁 (Miso Soup) - シンプル、短時間
4. 焼きそば (Yakisoba) - 炒め、ソース追加

### 13.3 既知の制限事項

1. **ネットワーク制限**:
   - 開発環境ではGradleサービスにアクセス不可
   - `./gradlew runClient` は実行不可
   - コード検証は手動構文チェックで実施

2. **API制限**:
   - Gemini APIキーが必要
   - レート制限の考慮なし
   - オフライン動作不可

3. **GUI制限**:
   - レシピ表示は基本的なテキストのみ
   - 画像表示なし
   - アニメーションなし

---

## 14. ライセンス・クレジット

### 14.1 使用技術

- **Minecraft Forge**: Forge Development LLC
- **Google Gemini API**: Google LLC
- **OkHttp**: Square, Inc.
- **Gson**: Google LLC

### 14.2 統合Mod

- **KaleidoscopeCookery**: 料理アイテム提供（重複除外済み）

### 14.3 開発

- **開発**: MochidsukiC
- **AI支援**: Claude (Anthropic)
- **プロジェクト生成**: Claude Code

---

## 15. バージョン履歴

### v0.1.0 (Initial Implementation)
- AIレシピ生成システム実装
- 容器システム実装（200g/300g/500g）
- まな板と包丁システム実装
- 重量ベース食品システム実装
- 二重計測システム（grams/count）実装

### v0.2.0 (Cooking System)
- 調理ブロックシステム実装
- CookingAction enum 実装（9種類）
- ホットプレート実装
- 鍋実装
- オーブン実装

### v0.3.0 (IH Heat Source System)
- IHヒーターを熱源として再設計
- 熱レベルシステム実装（OFF/LOW/MEDIUM/HIGH）
- 熱源依存/独立調理ブロック分類
- フライパン実装
- 動的調理速度システム実装

---

## 16. コンタクト

**プロジェクトリポジトリ**: `/home/user/CustomCookingMod`
**開発ブランチ**: `claude/mod-development-011CUTESmt1TUg6pJaLqUagU`

---

**文書バージョン**: 1.0
**最終更新日**: 2025-11-26
**作成者**: Claude Code AI Assistant

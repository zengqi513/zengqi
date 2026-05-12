# RoBERTa 意图识别模型部署指南

## 概述

使用 `hfl/chinese-roberta-wwm-ext` 轻量级模型实现意图识别，通过余弦相似度匹配意图模板。

**模型特点：**
- 参数量：110M
- ONNX FP32：~400MB
- ONNX INT8：~100MB
- 推理速度：~50ms（Android CPU）
- 架构：标准 BERT（无 GQA，ONNX Runtime 完全兼容）

---

## 第一步：导出模型

在有 Python 环境（安装了 transformers 和 torch）的机器上运行：

```bash
# 1. 安装依赖
pip install torch transformers onnx onnxruntime numpy

# 2. 运行导出脚本
cd E:\AutoBookkeeper
python roberta_intent_recognizer.py
```

**输出文件：**
```
roberta_model/
├── chinese-roberta-wwm-ext.onnx          # FP32 模型 (~400MB)
├── chinese-roberta-wwm-ext-int8.onnx     # INT8 量化模型 (~100MB) ⭐推荐
├── vocab.txt                             # 词汇表
├── tokenizer.json                        # Tokenizer 配置
├── tokenizer_config.json
└── intent_vectors.json                   # 预计算意图向量
```

---

## 第二步：复制到 Android 项目

将模型文件复制到 Android 项目的 assets 目录：

```bash
# 创建目录
mkdir -p app/src/main/assets/roberta

# 复制文件（只需 INT8 版本和词汇表）
cp roberta_model/chinese-roberta-wwm-ext-int8.onnx app/src/main/assets/roberta/
cp roberta_model/vocab.txt app/src/main/assets/roberta/
cp roberta_model/intent_vectors.json app/src/main/assets/roberta/
```

**最终目录结构：**
```
app/src/main/assets/
└── roberta/
    ├── chinese-roberta-wwm-ext-int8.onnx  (100MB)
    ├── vocab.txt                          (1MB)
    └── intent_vectors.json                (50KB)
```

---

## 第三步：构建 APK

```bash
./gradlew assembleDebug
```

**APK 体积增加：**
- 模型文件：~101MB
- 打包后压缩：~60-70MB

---

## 意图识别流程

```
用户输入: "这个月花了多少钱"
    ↓
SimpleTokenizer 分词: [CLS] 这 个 月 花 了 多 少 钱 [SEP]
    ↓
RoBERTa ONNX 推理 → 768维文本向量
    ↓
余弦相似度计算（与9个意图模板对比）
    ↓
匹配到: query_expense (相似度: 0.92)
    ↓
QueryExecutor 执行查询 → 返回结果
```

---

## 支持的意图

| 意图 | 示例查询 | 匹配模板 |
|------|---------|---------|
| query_expense | "本月支出多少" | 支出、花了、消费 |
| query_income | "本月收入多少" | 收入、赚了多少 |
| query_budget | "预算还剩多少" | 预算、剩余 |
| query_trend | "和上月比怎么样" | 环比、趋势 |
| query_ranking | "支出最多的是什么" | 最多、占比、排行 |
| query_average | "平均每天花多少" | 平均、日均 |
| query_count | "多少笔交易" | 笔数、数量 |
| query_anomaly | "有没有异常" | 异常、不正常 |
| query_suggestion | "有什么建议" | 建议、推荐 |

---

## 添加新意图

编辑 `roberta_intent_recognizer.py` 中的 `INTENT_TEMPLATES`：

```python
INTENT_TEMPLATES = {
    "query_expense": [...],
    "query_new_intent": [  # 新增意图
        "新查询模板1",
        "新查询模板2",
        "新查询模板3"
    ]
}
```

然后重新运行导出脚本。

---

## 性能优化

### 1. 首次加载优化
- 模型在 `Application.onCreate()` 中异步加载
- 使用 `viewModelScope` 避免阻塞 UI

### 2. 推理优化
- INT8 量化减少 75% 模型体积
- 使用 2 线程并行推理
- 最大序列长度 32（意图查询通常很短）

### 3. 回退机制
```
RoBERTa 失败 → 规则引擎 → 默认回复
```

---

## 故障排查

### 模型加载失败
```
检查: assets/roberta/ 目录是否存在
检查: 文件大小是否正确（int8 ~100MB）
```

### 词汇表加载失败
```
检查: vocab.txt 是否存在
检查: 文件编码是否为 UTF-8
```

### 意图识别不准确
```
1. 检查相似度阈值（默认 0.75）
2. 添加更多意图模板
3. 检查 tokenizer 分词是否正确
```

---

## 对比其他方案

| 方案 | 体积 | 速度 | 准确率 | 开发成本 |
|------|------|------|--------|---------|
| 规则引擎 | 0MB | 极快 | 70% | ⭐⭐⭐⭐⭐ |
| **RoBERTa + 相似度** | **100MB** | **快** | **85%** | **⭐⭐⭐⭐** |
| RoBERTa + 微调 | 100MB | 快 | 92% | ⭐⭐⭐ |
| Qwen2 (ONNX) | 1800MB | 慢 | 95% | ⭐⭐ |
| MNN-LLM | 400MB | 快 | 95% | ⭐⭐⭐ |

---

## 下一步

1. ✅ 导出模型并放入 assets
2. ✅ 构建 APK 测试
3. ✅ 收集用户反馈
4. （可选）微调模型提升准确率

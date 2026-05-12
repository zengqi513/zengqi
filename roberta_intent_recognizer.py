#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RoBERTa 意图识别模型导出脚本
在安装了 transformers 和 torch 的环境中运行

运行方式:
    python roberta_intent_recognizer.py

输出:
    - roberta_model/chinese-roberta-wwm-ext.onnx (FP32)
    - roberta_model/chinese-roberta-wwm-ext-int8.onnx (INT8 量化)
    - roberta_model/vocab.txt (tokenizer 词汇表)
"""

import os
import json

try:
    import torch
    from transformers import AutoTokenizer, AutoModel
    import numpy as np
except ImportError:
    print("请先安装依赖:")
    print("  pip install torch transformers numpy")
    exit(1)

# ============ 配置 ============
MODEL_NAME = "hfl/chinese-roberta-wwm-ext"
MAX_LENGTH = 32
OUTPUT_DIR = "./roberta_model"

# 意图模板定义（用于后续相似度匹配）
INTENT_TEMPLATES = {
    "query_expense": [
        "本月支出多少",
        "花了多少钱",
        "消费多少",
        "用了多少钱",
        "支出统计",
        "花了多少",
        "开销多少"
    ],
    "query_income": [
        "本月收入多少",
        "赚了多少钱",
        "进账多少",
        "收入统计",
        "赚了多少"
    ],
    "query_budget": [
        "预算还剩多少",
        "还剩多少预算",
        "预算余额",
        "预算剩余",
        "预算还有多少"
    ],
    "query_trend": [
        "比上月怎么样",
        "环比变化",
        "趋势如何",
        "和上周比",
        "变化情况"
    ],
    "query_ranking": [
        "支出最多的是什么",
        "占比多少",
        "哪个分类最多",
        "消费排行",
        "支出排名"
    ],
    "query_average": [
        "平均每天花多少",
        "日均消费",
        "平均支出",
        "每天多少钱"
    ],
    "query_count": [
        "多少笔交易",
        "交易笔数",
        "多少笔支出",
        "记录数量"
    ],
    "query_anomaly": [
        "有没有异常",
        "异常消费",
        "异常支出",
        "不正常的消费"
    ],
    "query_suggestion": [
        "有什么建议",
        "理财建议",
        "省钱建议",
        "消费建议"
    ]
}

os.makedirs(OUTPUT_DIR, exist_ok=True)

print(f"正在加载模型: {MODEL_NAME}")
print("-" * 50)

# 加载模型
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModel.from_pretrained(MODEL_NAME)
model.eval()

print(f"✓ 模型加载完成")
print(f"  参数量: {sum(p.numel() for p in model.parameters()) / 1e6:.1f}M")

# 导出 ONNX
print(f"\n导出 ONNX 模型...")
onnx_path = os.path.join(OUTPUT_DIR, "chinese-roberta-wwm-ext.onnx")

dummy_inputs = tokenizer(
    "测试文本",
    return_tensors="pt",
    padding="max_length",
    max_length=MAX_LENGTH,
    truncation=True
)

torch.onnx.export(
    model,
    (dummy_inputs['input_ids'], dummy_inputs['attention_mask']),
    onnx_path,
    input_names=['input_ids', 'attention_mask'],
    output_names=['last_hidden_state', 'pooler_output'],
    dynamic_axes={
        'input_ids': {0: 'batch_size'},
        'attention_mask': {0: 'batch_size'},
        'last_hidden_state': {0: 'batch_size'},
        'pooler_output': {0: 'batch_size'}
    },
    opset_version=13
)

print(f"✓ ONNX 导出完成: {onnx_path}")
print(f"  大小: {os.path.getsize(onnx_path) / 1024 / 1024:.1f} MB")

# INT8 量化
try:
    from onnxruntime.quantization import quantize_dynamic, QuantType
    
    quantized_path = os.path.join(OUTPUT_DIR, "chinese-roberta-wwm-ext-int8.onnx")
    quantize_dynamic(onnx_path, quantized_path, weight_type=QuantType.QInt8)
    
    print(f"✓ INT8 量化完成: {quantized_path}")
    print(f"  大小: {os.path.getsize(quantized_path) / 1024 / 1024:.1f} MB")
except Exception as e:
    print(f"✗ 量化失败: {e}")

# 保存 tokenizer
print(f"\n保存 tokenizer...")
tokenizer.save_pretrained(OUTPUT_DIR)

# 保存词汇表为 txt（方便 Android 读取）
vocab_path = os.path.join(OUTPUT_DIR, "vocab.txt")
with open(vocab_path, "w", encoding="utf-8") as f:
    for token, idx in sorted(tokenizer.vocab.items(), key=lambda x: x[1]):
        f.write(f"{token}\n")
print(f"✓ 词汇表保存: {vocab_path}")

# 预计算意图模板向量
print(f"\n预计算意图模板向量...")
import onnxruntime as ort

session = ort.InferenceSession(onnx_path)
intent_vectors = {}

for intent, templates in INTENT_TEMPLATES.items():
    vectors = []
    for template in templates:
        inputs = tokenizer(
            template,
            return_tensors="np",
            padding="max_length",
            max_length=MAX_LENGTH,
            truncation=True
        )
        outputs = session.run(None, {
            'input_ids': inputs['input_ids'],
            'attention_mask': inputs['attention_mask']
        })
        # 使用 pooler_output (768维)
        vectors.append(outputs[1][0].tolist())
    
    # 计算平均向量
    avg_vector = np.mean(vectors, axis=0)
    intent_vectors[intent] = avg_vector.tolist()
    print(f"  {intent}: {len(templates)} 个模板")

# 保存意图向量
vectors_path = os.path.join(OUTPUT_DIR, "intent_vectors.json")
with open(vectors_path, "w", encoding="utf-8") as f:
    json.dump({
        "intents": list(INTENT_TEMPLATES.keys()),
        "vectors": intent_vectors,
        "dim": 768
    }, f, ensure_ascii=False, indent=2)
print(f"✓ 意图向量保存: {vectors_path}")

print(f"\n{'='*50}")
print("导出完成！")
print(f"输出目录: {OUTPUT_DIR}")
print(f"{'='*50}")

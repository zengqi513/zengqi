#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
导出 chinese-roberta-wwm-ext 为 ONNX 格式
用于 AutoBookkeeper 意图识别
"""

import os
import sys

# 检查依赖
try:
    import torch
    import transformers
    from transformers import AutoTokenizer, AutoModel
    print(f"✓ PyTorch version: {torch.__version__}")
    print(f"✓ Transformers version: {transformers.__version__}")
except ImportError as e:
    print(f"✗ 缺少依赖: {e}")
    print("请安装: pip install torch transformers onnx onnxruntime")
    sys.exit(1)

# 模型配置
MODEL_NAME = "hfl/chinese-roberta-wwm-ext"
MAX_LENGTH = 32  # 意图识别文本较短
OUTPUT_DIR = "./roberta_model"

os.makedirs(OUTPUT_DIR, exist_ok=True)

print(f"\n正在下载/加载模型: {MODEL_NAME}")
print("-" * 50)

# 加载 tokenizer 和模型
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModel.from_pretrained(MODEL_NAME)
model.eval()

print(f"✓ 模型加载完成")
print(f"  - 参数量: {sum(p.numel() for p in model.parameters()) / 1e6:.1f}M")
print(f"  - 隐藏层维度: {model.config.hidden_size}")
print(f"  - 层数: {model.config.num_hidden_layers}")

# 准备 dummy input
dummy_text = "本月支出多少"
inputs = tokenizer(
    dummy_text,
    return_tensors="pt",
    padding="max_length",
    max_length=MAX_LENGTH,
    truncation=True
)

print(f"\n示例输入: '{dummy_text}'")
print(f"  - input_ids shape: {inputs['input_ids'].shape}")
print(f"  - attention_mask shape: {inputs['attention_mask'].shape}")

# 导出 ONNX
onnx_path = os.path.join(OUTPUT_DIR, "chinese-roberta-wwm-ext.onnx")
print(f"\n导出 ONNX 模型...")

torch.onnx.export(
    model,
    (inputs['input_ids'], inputs['attention_mask']),
    onnx_path,
    input_names=['input_ids', 'attention_mask'],
    output_names=['last_hidden_state', 'pooler_output'],
    dynamic_axes={
        'input_ids': {0: 'batch_size'},
        'attention_mask': {0: 'batch_size'},
        'last_hidden_state': {0: 'batch_size', 1: 'sequence_length'},
        'pooler_output': {0: 'batch_size'}
    },
    opset_version=13,
    do_constant_folding=True
)

print(f"✓ ONNX 模型导出完成: {onnx_path}")
print(f"  - 文件大小: {os.path.getsize(onnx_path) / 1024 / 1024:.1f} MB")

# INT8 量化
try:
    from onnxruntime.quantization import quantize_dynamic, QuantType
    
    print(f"\n进行 INT8 量化...")
    quantized_path = os.path.join(OUTPUT_DIR, "chinese-roberta-wwm-ext-int8.onnx")
    
    quantize_dynamic(
        onnx_path,
        quantized_path,
        weight_type=QuantType.QInt8
    )
    
    print(f"✓ INT8 量化完成: {quantized_path}")
    print(f"  - 文件大小: {os.path.getsize(quantized_path) / 1024 / 1024:.1f} MB")
    
except ImportError:
    print("✗ 跳过量化 (未安装 onnxruntime)")

# 保存 tokenizer
print(f"\n保存 tokenizer...")
tokenizer.save_pretrained(OUTPUT_DIR)
print(f"✓ Tokenizer 保存完成")

# 测试推理
try:
    import onnxruntime as ort
    
    print(f"\n测试 ONNX 推理...")
    session = ort.InferenceSession(onnx_path)
    
    # 准备输入
    test_text = "预算还剩多少"
    test_inputs = tokenizer(
        test_text,
        return_tensors="np",
        padding="max_length",
        max_length=MAX_LENGTH,
        truncation=True
    )
    
    # 推理
    outputs = session.run(
        None,
        {
            'input_ids': test_inputs['input_ids'],
            'attention_mask': test_inputs['attention_mask']
        }
    )
    
    last_hidden_state = outputs[0]
    pooler_output = outputs[1]
    
    print(f"✓ 推理测试通过")
    print(f"  - 输入: '{test_text}'")
    print(f"  - last_hidden_state shape: {last_hidden_state.shape}")
    print(f"  - pooler_output shape: {pooler_output.shape}")
    print(f"  - [CLS] vector (前5维): {pooler_output[0, :5]}")
    
except ImportError:
    print("✗ 跳过推理测试 (未安装 onnxruntime)")

print(f"\n{'='*50}")
print(f"导出完成！")
print(f"模型文件位置: {OUTPUT_DIR}")
print(f"{'='*50}")

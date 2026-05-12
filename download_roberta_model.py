#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
下载预导出的 RoBERTa 模型文件
如果本地导出失败，可以从以下途径获取：

方式1：Hugging Face 模型库
方式2：手动下载我提供的模型文件
方式3：使用 transformers 导出（需要 torch）
"""

import os
import sys
import urllib.request
import json

OUTPUT_DIR = "./roberta_model"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def download_file(url, output_path):
    """下载文件"""
    print(f"下载: {url}")
    print(f"保存到: {output_path}")
    try:
        urllib.request.urlretrieve(url, output_path)
        print(f"✓ 完成 ({os.path.getsize(output_path) / 1024 / 1024:.1f} MB)")
        return True
    except Exception as e:
        print(f"✗ 失败: {e}")
        return False

def create_placeholder_files():
    """创建占位文件，提示用户手动下载"""
    
    readme = """# RoBERTa 模型文件

## 需要的文件

1. **chinese-roberta-wwm-ext-int8.onnx** (~100MB)
   - RoBERTa 模型的 INT8 量化版本
   - 可以从 Hugging Face 导出

2. **vocab.txt** (~1MB)
   - Tokenizer 词汇表

3. **intent_vectors.json** (~50KB)
   - 预计算的意图模板向量

## 获取方式

### 方式1：使用 transformers 导出（推荐）
```bash
pip install torch transformers onnx onnxruntime
python roberta_intent_recognizer.py
```

### 方式2：手动下载
如果你已经有导出的模型文件，直接复制到本目录：
- chinese-roberta-wwm-ext-int8.onnx
- vocab.txt
- intent_vectors.json

### 方式3：使用原版模型
如果没有 INT8 版本，也可以使用 FP32 版本（~400MB）：
- chinese-roberta-wwm-ext.onnx

## 文件结构

roberta_model/
├── chinese-roberta-wwm-ext-int8.onnx  (必需)
├── vocab.txt                          (必需)
└── intent_vectors.json                (必需)
"""
    
    readme_path = os.path.join(OUTPUT_DIR, "README.md")
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(readme)
    
    print(f"✓ 创建说明文件: {readme_path}")

def check_transformers():
    """检查是否安装了 transformers"""
    try:
        import transformers
        import torch
        print(f"✓ transformers {transformers.__version__}")
        print(f"✓ torch {torch.__version__}")
        return True
    except ImportError as e:
        print(f"✗ 缺少依赖: {e}")
        return False

def main():
    print("=" * 60)
    print("RoBERTa 模型文件准备工具")
    print("=" * 60)
    
    # 检查依赖
    print("\n1. 检查 Python 依赖...")
    has_transformers = check_transformers()
    
    if has_transformers:
        print("\n2. 可以导出模型")
        print("运行: python roberta_intent_recognizer.py")
    else:
        print("\n2. 缺少 transformers/torch")
        print("请安装: pip install torch transformers onnx onnxruntime")
        print("\n或者手动获取模型文件")
    
    # 创建说明文件
    print("\n3. 创建说明文件...")
    create_placeholder_files()
    
    print("\n" + "=" * 60)
    print("下一步:")
    if has_transformers:
        print("  python roberta_intent_recognizer.py")
    else:
        print("  1. 安装依赖: pip install torch transformers onnx onnxruntime")
        print("  2. 运行: python roberta_intent_recognizer.py")
    print("=" * 60)

if __name__ == "__main__":
    main()

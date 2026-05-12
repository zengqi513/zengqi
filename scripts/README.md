# AutoBookkeeper 自动发布脚本

## 文件说明

| 文件 | 用途 |
|------|------|
| `release.ps1` | 主发布脚本（核心逻辑） |
| `publish.bat` | Windows双击运行（交互式） |
| `publish.ps1` | PowerShell一键发布 |

## 使用方法

### 方法一：双击运行（推荐）
1. 双击 `publish.bat`
2. 输入版本号（如：2.1.1）
3. 输入GitHub Personal Access Token
4. 确认发布，等待完成

### 方法二：命令行
```powershell
# 使用保存的Token
.\publish.ps1 -Version "2.1.1"

# 直接指定Token
.\publish.ps1 -Version "2.1.1" -Token "ghp_xxxxxxxx"

# 使用主脚本（更多参数）
.\release.ps1 -Version "2.1.1" -Token "ghp_xxxxxxxx"
```

## 脚本功能

自动完成以下步骤：
1. ✅ 读取当前版本号
2. ✅ 自动递增版本号
3. ✅ 构建Release APK
4. ✅ 复制APK到发布目录
5. ✅ 提交版本更新到Git
6. ✅ 推送代码到GitHub
7. ✅ 创建GitHub Release
8. ✅ 上传APK文件

## Token配置

### 生成GitHub Token
1. 访问：https://github.com/settings/tokens/new
2. 勾选 `repo` 权限
3. 生成并复制Token

### 保存Token（可选）
脚本会询问是否保存Token，保存后下次无需重复输入。

Token保存在 `.github_token` 文件中，请确保该文件不被泄露。

## 注意事项

- 确保已安装Android SDK和Gradle
- 确保Java环境变量配置正确
- 首次运行可能需要下载依赖，耗时较长
- APK文件较大（约100MB），上传需要几分钟

## 故障排除

### 构建失败
```powershell
# 清理构建缓存
.\gradlew clean
```

### 推送失败
```powershell
# 检查Git配置
git remote -v
git status
```

### 上传失败
- 检查Token是否有效
- 检查网络连接
- 检查仓库权限

## 版本命名规范

- 格式：`主版本.次版本.修订号`
- 示例：`2.1.0`, `2.1.1`, `2.2.0`
- 重大更新递增主版本
- 功能更新递增次版本
- Bug修复递增修订号

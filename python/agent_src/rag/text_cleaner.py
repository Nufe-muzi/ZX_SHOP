"""
文本清洗模块
支持多种文本清洗功能：压缩空格、压缩换行、移除空行等
"""

import re
from typing import Dict, Any


class TextCleaner:
    """文本清洗器"""

    def __init__(
        self,
        compress_spaces: bool = True,
        compress_newlines: bool = True,
        remove_empty_lines: bool = False,
        trim_lines: bool = True
    ):
        """
        初始化文本清洗器

        Args:
            compress_spaces: 压缩连续空格为单个空格
            compress_newlines: 压缩连续换行为最多两个
            remove_empty_lines: 移除空行
            trim_lines: 去除每行首尾空白
        """
        self.compress_spaces = compress_spaces
        self.compress_newlines = compress_newlines
        self.remove_empty_lines = remove_empty_lines
        self.trim_lines = trim_lines

    def clean(self, text: str) -> str:
        """
        清洗文本

        Args:
            text: 待清洗的文本

        Returns:
            清洗后的文本
        """
        if not text:
            return text

        result = text

        # 1. 去除每行首尾空白
        if self.trim_lines:
            lines = result.split('\n')
            lines = [line.strip() for line in lines]
            result = '\n'.join(lines)

        # 2. 压缩连续空格
        if self.compress_spaces:
            result = re.sub(r'[ \t]+', ' ', result)

        # 3. 移除空行
        if self.remove_empty_lines:
            lines = result.split('\n')
            lines = [line for line in lines if line.strip()]
            result = '\n'.join(lines)

        # 4. 压缩连续换行（保留段落结构）
        if self.compress_newlines:
            # 将3个及以上连续换行压缩为2个
            result = re.sub(r'\n{3,}', '\n\n', result)

        return result.strip()

    def clean_documents(self, documents: list) -> list:
        """
        清洗文档列表

        Args:
            documents: Document 对象列表

        Returns:
            清洗后的 Document 对象列表
        """
        cleaned_docs = []
        for doc in documents:
            cleaned_content = self.clean(doc.page_content)
            if cleaned_content.strip():  # 只保留非空文档
                # 复制元数据
                new_doc = type(doc)(
                    page_content=cleaned_content,
                    metadata=doc.metadata.copy() if hasattr(doc, 'metadata') else {}
                )
                cleaned_docs.append(new_doc)
        return cleaned_docs


def create_cleaner(settings: Dict[str, Any] = None) -> TextCleaner:
    """
    创建文本清洗器的便捷函数

    Args:
        settings: 清洗设置字典

    Returns:
        TextCleaner 实例
    """
    if settings is None:
        settings = {}

    return TextCleaner(
        compress_spaces=settings.get('compress_spaces', True),
        compress_newlines=settings.get('compress_newlines', True),
        remove_empty_lines=settings.get('remove_empty_lines', False),
        trim_lines=settings.get('trim_lines', True)
    )


def clean_text(
    text: str,
    compress_spaces: bool = True,
    compress_newlines: bool = True,
    remove_empty_lines: bool = False,
    trim_lines: bool = True
) -> str:
    """
    清洗文本的便捷函数

    Args:
        text: 待清洗的文本
        compress_spaces: 压缩连续空格
        compress_newlines: 压缩连续换行
        remove_empty_lines: 移除空行
        trim_lines: 去除每行首尾空白

    Returns:
        清洗后的文本
    """
    cleaner = TextCleaner(
        compress_spaces=compress_spaces,
        compress_newlines=compress_newlines,
        remove_empty_lines=remove_empty_lines,
        trim_lines=trim_lines
    )
    return cleaner.clean(text)

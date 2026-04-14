"""
文本切分器模块
支持多种切分策略：递归切分、字符切分、语义切分等
"""

from typing import List, Optional, Callable
from langchain_text_splitters import (
    RecursiveCharacterTextSplitter,
    CharacterTextSplitter,
    TokenTextSplitter
)
from langchain_core.documents import Document


class TextSplitter:
    """文本切分器"""

    def __init__(
        self,
        chunk_size: int = 800,
        chunk_overlap: int = 80,
        separators: Optional[List[str]] = None,
        length_function: Callable[[str], int] = len
    ):
        """
        初始化切分器

        Args:
            chunk_size: 分段最大长度
            chunk_overlap: 分段重叠字符数
            separators: 分隔符列表（按优先级排序）
            length_function: 计算文本长度的函数
        """
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.separators = separators or self._get_default_separators()
        self.length_function = length_function

    def _get_default_separators(self) -> List[str]:
        """获取默认分隔符（Dify 风格）"""
        return [
            "\n\n",     # 段落分隔
            "\n",       # 换行
            "。",       # 中文句号
            "！",       # 中文感叹号
            "？",       # 中文问号
            "；",       # 中文分号
            "……",      # 中文省略号
            ".",        # 英文句号
            "!",        # 英文感叹号
            "?",        # 英文问号
            ";",        # 英文分号
            " ",        # 空格
            ""          # 字符边界
        ]

    def get_preset_separators(self, separator_type: str) -> List[str]:
        """
        获取预设分隔符

        Args:
            separator_type: 分隔符类型
                - "newline": 换行
                - "paragraph": 段落
                - "sentence": 句子
                - "space": 空格

        Returns:
            分隔符列表
        """
        presets = {
            "newline": ["\n", " ", ""],
            "paragraph": ["\n\n", "\n", " ", ""],
            "sentence": [
                "\n\n", "\n",
                "。", "！", "？", "；",
                ".", "!", "?", ";",
                " ", ""
            ],
            "space": [" ", ""]
        }
        return presets.get(separator_type, self._get_default_separators())

    def split_text(self, text: str, mode: str = "recursive") -> List[str]:
        """
        切分文本

        Args:
            text: 待切分的文本
            mode: 切分模式 ("recursive" 或 "character")

        Returns:
            切分后的文本片段列表
        """
        if mode == "recursive":
            splitter = RecursiveCharacterTextSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                separators=self.separators,
                length_function=self.length_function,
                is_separator_regex=False
            )
        else:
            splitter = CharacterTextSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                separator=self.separators[0] if self.separators else "\n\n"
            )

        return splitter.split_text(text)

    def split_documents(
        self,
        documents: List[Document],
        mode: str = "recursive"
    ) -> List[Document]:
        """
        切分文档列表

        Args:
            documents: Document 对象列表
            mode: 切分模式

        Returns:
            切分后的 Document 对象列表
        """
        if mode == "recursive":
            splitter = RecursiveCharacterTextSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                separators=self.separators,
                length_function=self.length_function,
                is_separator_regex=False
            )
        else:
            splitter = CharacterTextSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                separator=self.separators[0] if self.separators else "\n\n"
            )

        return splitter.split_documents(documents)

    def preview_split(
        self,
        text: str,
        mode: str = "recursive"
    ) -> List[dict]:
        """
        预览切分结果

        Args:
            text: 待切分的文本
            mode: 切分模式

        Returns:
            包含切分信息的字典列表
        """
        chunks = self.split_text(text, mode)
        results = []

        for i, chunk in enumerate(chunks):
            results.append({
                "index": i + 1,
                "content": chunk,
                "char_count": len(chunk),
                "separator_hint": self._detect_separator(chunk)
            })

        return results

    def _detect_separator(self, text: str) -> str:
        """检测文本片段末尾的分隔符类型"""
        if not text:
            return "无"

        last_chars = text[-10:] if len(text) >= 10 else text

        if "\n\n" in last_chars:
            return "段落分隔(\\n\\n)"
        elif "\n" in last_chars:
            return "换行分隔(\\n)"
        elif "。" in last_chars:
            return "中文句号(。)"
        elif "！" in last_chars or "!" in last_chars:
            return "感叹号"
        elif "？" in last_chars or "?" in last_chars:
            return "问号"
        elif "；" in last_chars or ";" in last_chars:
            return "分号"
        elif "." in last_chars:
            return "英文句号(.)"
        else:
            return "字符边界"


class SemanticSplitter:
    """语义切分器（基于句子边界）"""

    def __init__(
        self,
        chunk_size: int = 800,
        chunk_overlap: int = 80
    ):
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.sentence_endings = ["。", "！", "？", ".", "!", "?"]

    def split_text(self, text: str) -> List[str]:
        """按句子边界切分文本"""
        sentences = self._split_into_sentences(text)
        chunks = []
        current_chunk = ""

        for sentence in sentences:
            if len(current_chunk) + len(sentence) <= self.chunk_size:
                current_chunk += sentence
            else:
                if current_chunk:
                    chunks.append(current_chunk.strip())
                current_chunk = sentence

        if current_chunk:
            chunks.append(current_chunk.strip())

        return chunks

    def _split_into_sentences(self, text: str) -> List[str]:
        """将文本分割成句子"""
        import re
        pattern = f'([{"|".join(self.sentence_endings)}])'
        parts = re.split(pattern, text)

        sentences = []
        for i in range(0, len(parts) - 1, 2):
            if i + 1 < len(parts):
                sentences.append(parts[i] + parts[i + 1])
            else:
                sentences.append(parts[i])

        if len(parts) % 2 == 1 and parts[-1]:
            sentences.append(parts[-1])

        return [s for s in sentences if s.strip()]


class TokenSplitter:
    """基于 Token 的切分器"""

    def __init__(
        self,
        chunk_size: int = 500,
        chunk_overlap: int = 50,
        encoding_name: str = "cl100k_base"
    ):
        """
        初始化 Token 切分器

        Args:
            chunk_size: 每个 chunk 的最大 token 数
            chunk_overlap: chunk 之间的 token 重叠数
            encoding_name: tiktoken 编码名称
        """
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.encoding_name = encoding_name

    def split_text(self, text: str) -> List[str]:
        """按 Token 数切分文本"""
        try:
            splitter = TokenTextSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                encoding_name=self.encoding_name
            )
            return splitter.split_text(text)
        except ImportError:
            # 如果没有安装 tiktoken，回退到字符切分
            return TextSplitter(
                chunk_size=self.chunk_size * 4,  # 粗略估计
                chunk_overlap=self.chunk_overlap * 4
            ).split_text(text)


def create_splitter(
    chunk_size: int = 800,
    chunk_overlap: int = 80,
    separator_type: str = "newline",
    custom_separators: Optional[List[str]] = None
) -> TextSplitter:
    """
    创建文本切分器的便捷函数

    Args:
        chunk_size: 分段最大长度
        chunk_overlap: 分段重叠字符数
        separator_type: 预设分隔符类型
        custom_separators: 自定义分隔符列表

    Returns:
        TextSplitter 实例
    """
    splitter = TextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap
    )

    if custom_separators:
        splitter.separators = custom_separators
    else:
        splitter.separators = splitter.get_preset_separators(separator_type)

    return splitter

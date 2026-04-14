"""
文件处理器模块
整合文档加载、清洗和切分功能
"""

import os
import tempfile
from typing import List, Optional, Dict, Any
from langchain_core.documents import Document

from agent_src.rag.document_loaders import (
    load_document,
    get_loader,
    get_supported_formats
)
from agent_src.rag.text_splitters import (
    TextSplitter,
    create_splitter,
    SemanticSplitter
)
from agent_src.rag.text_cleaner import TextCleaner, create_cleaner


class FileProcessor:
    """文件处理器"""

    def __init__(
        self,
        chunk_size: int = 500,
        chunk_overlap: int = 80,
        separator_type: str = "newline",
        custom_separators: Optional[List[str]] = None,
        clean_settings: Optional[Dict[str, Any]] = None
    ):
        """
        初始化文件处理器

        Args:
            chunk_size: 分段最大长度
            chunk_overlap: 分段重叠字符数
            separator_type: 分隔符类型
            custom_separators: 自定义分隔符列表
            clean_settings: 清洗设置
        """
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.separator_type = separator_type
        self.custom_separators = custom_separators
        self.clean_settings = clean_settings or {}
        self.splitter = create_splitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separator_type=separator_type,
            custom_separators=custom_separators
        )
        self.cleaner = create_cleaner(self.clean_settings)

    def process_file(
        self,
        file_path: str,
        mode: str = "recursive"
    ) -> List[Document]:
        """
        处理单个文件：加载 + 清洗 + 切分

        Args:
            file_path: 文件路径
            mode: 切分模式

        Returns:
            切分后的 Document 列表
        """
        # 加载文档
        documents = load_document(file_path)

        # 清洗文档
        documents = self.cleaner.clean_documents(documents)

        # 切分文档
        split_docs = self.splitter.split_documents(documents, mode)

        return split_docs

    def process_uploaded_file(
        self,
        file_content: bytes,
        filename: str,
        mode: str = "recursive"
    ) -> List[Document]:
        """
        处理上传的文件

        Args:
            file_content: 文件二进制内容
            filename: 文件名
            mode: 切分模式

        Returns:
            切分后的 Document 列表
        """
        # 保存到临时文件
        ext = os.path.splitext(filename)[1].lower()
        with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
            tmp.write(file_content)
            tmp_path = tmp.name

        try:
            # 处理文件
            documents = self.process_file(tmp_path, mode)

            # 更新元数据
            for doc in documents:
                doc.metadata["source"] = filename

            return documents
        finally:
            # 清理临时文件
            os.remove(tmp_path)

    def preview_file(
        self,
        file_path: str,
        mode: str = "recursive"
    ) -> Dict[str, Any]:
        """
        预览文件切分结果

        Args:
            file_path: 文件路径
            mode: 切分模式

        Returns:
            包含预览信息的字典
        """
        # 加载文档
        documents = load_document(file_path)

        # 清洗文档
        documents = self.cleaner.clean_documents(documents)

        # 获取清洗后内容
        cleaned_content = "\n".join([doc.page_content for doc in documents])

        # 预览切分
        preview_results = self.splitter.preview_split(cleaned_content, mode)

        return {
            "file_path": file_path,
            "total_chars": len(cleaned_content),
            "chunk_count": len(preview_results),
            "chunks": preview_results,
            "config": {
                "chunk_size": self.chunk_size,
                "chunk_overlap": self.chunk_overlap,
                "separator_type": self.separator_type,
                "clean_settings": self.clean_settings
            }
        }

    def update_settings(
        self,
        chunk_size: Optional[int] = None,
        chunk_overlap: Optional[int] = None,
        separator_type: Optional[str] = None,
        custom_separators: Optional[List[str]] = None,
        clean_settings: Optional[Dict[str, Any]] = None
    ):
        """
        更新切分设置

        Args:
            chunk_size: 分段最大长度
            chunk_overlap: 分段重叠字符数
            separator_type: 分隔符类型
            custom_separators: 自定义分隔符列表
            clean_settings: 清洗设置
        """
        if chunk_size is not None:
            self.chunk_size = chunk_size
        if chunk_overlap is not None:
            self.chunk_overlap = chunk_overlap
        if separator_type is not None:
            self.separator_type = separator_type
        if custom_separators is not None:
            self.custom_separators = custom_separators
        if clean_settings is not None:
            self.clean_settings = clean_settings
            self.cleaner = create_cleaner(clean_settings)

        # 重新创建切分器
        self.splitter = create_splitter(
            chunk_size=self.chunk_size,
            chunk_overlap=self.chunk_overlap,
            separator_type=self.separator_type,
            custom_separators=self.custom_separators
        )


class BatchProcessor:
    """批量文件处理器"""

    def __init__(self, file_processor: FileProcessor):
        self.file_processor = file_processor

    def process_directory(
        self,
        directory_path: str,
        recursive: bool = True
    ) -> List[Document]:
        """
        处理目录下的所有文件

        Args:
            directory_path: 目录路径
            recursive: 是否递归处理子目录

        Returns:
            所有文档的切分结果
        """
        all_documents = []
        supported_formats = get_supported_formats()

        for root, dirs, files in os.walk(directory_path):
            if not recursive:
                dirs.clear()

            for file in files:
                ext = os.path.splitext(file)[1].lower()
                if ext in supported_formats:
                    file_path = os.path.join(root, file)
                    try:
                        docs = self.file_processor.process_file(file_path)
                        all_documents.extend(docs)
                    except Exception as e:
                        print(f"处理文件 {file_path} 失败: {e}")

        return all_documents

    def process_files(
        self,
        file_paths: List[str]
    ) -> List[Document]:
        """
        批量处理文件列表

        Args:
            file_paths: 文件路径列表

        Returns:
            所有文档的切分结果
        """
        all_documents = []

        for file_path in file_paths:
            try:
                docs = self.file_processor.process_file(file_path)
                all_documents.extend(docs)
            except Exception as e:
                print(f"处理文件 {file_path} 失败: {e}")

        return all_documents


def get_file_info(file_path: str) -> Dict[str, Any]:
    """
    获取文件信息

    Args:
        file_path: 文件路径

    Returns:
        文件信息字典
    """
    ext = os.path.splitext(file_path)[1].lower()
    supported_formats = get_supported_formats()

    return {
        "path": file_path,
        "name": os.path.basename(file_path),
        "extension": ext,
        "size": os.path.getsize(file_path),
        "supported": ext in supported_formats
    }

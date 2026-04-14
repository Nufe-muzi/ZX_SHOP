"""
数据库模块
支持 PostgreSQL 存储知识库和文档片段
"""

import os
from typing import List, Optional, Dict, Any
from datetime import datetime
import json
import asyncio

# 数据库连接配置
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/rag_db")

# 全局连接池
_pool = None


async def get_pool():
    """获取数据库连接池"""
    global _pool
    if _pool is None:
        try:
            import asyncpg
            _pool = await asyncpg.create_pool(DATABASE_URL, min_size=2, max_size=10)
        except Exception as e:
            print(f"数据库连接失败: {e}")
            return None
    return _pool


async def init_db():
    """初始化数据库表"""
    pool = await get_pool()
    if pool is None:
        return False

    async with pool.acquire() as conn:
        # 创建知识库表
        await conn.execute('''
            CREATE TABLE IF NOT EXISTS knowledge_bases (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                description TEXT,
                embedding_model VARCHAR(100) DEFAULT 'text-embedding-v2',
                chunk_size INTEGER DEFAULT 800,
                chunk_overlap INTEGER DEFAULT 80,
                separator_type VARCHAR(50) DEFAULT 'newline',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        # 创建文档表
        await conn.execute('''
            CREATE TABLE IF NOT EXISTS documents (
                id SERIAL PRIMARY KEY,
                kb_id INTEGER REFERENCES knowledge_bases(id) ON DELETE CASCADE,
                filename VARCHAR(255) NOT NULL,
                file_type VARCHAR(50),
                file_size INTEGER,
                total_chars INTEGER,
                chunk_count INTEGER,
                clean_settings JSONB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        # 创建文档片段表
        await conn.execute('''
            CREATE TABLE IF NOT EXISTS document_chunks (
                id SERIAL PRIMARY KEY,
                doc_id INTEGER REFERENCES documents(id) ON DELETE CASCADE,
                chunk_index INTEGER NOT NULL,
                content TEXT NOT NULL,
                char_count INTEGER,
                separator_hint VARCHAR(100),
                metadata JSONB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        # 创建索引
        await conn.execute('''
            CREATE INDEX IF NOT EXISTS idx_documents_kb_id ON documents(kb_id);
            CREATE INDEX IF NOT EXISTS idx_chunks_doc_id ON document_chunks(doc_id);
        ''')

    return True


class KnowledgeBaseManager:
    """知识库管理器"""

    def __init__(self):
        self.pool = None

    async def _get_pool(self):
        if self.pool is None:
            import asyncpg
            self.pool = await asyncpg.create_pool(DATABASE_URL, min_size=2, max_size=10)
        return self.pool

    async def create_kb(
        self,
        name: str,
        description: str = "",
        embedding_model: str = "text-embedding-v2",
        chunk_size: int = 800,
        chunk_overlap: int = 80,
        separator_type: str = "newline"
    ) -> Dict[str, Any]:
        """创建知识库"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            row = await conn.fetchrow('''
                INSERT INTO knowledge_bases (name, description, embedding_model, chunk_size, chunk_overlap, separator_type)
                VALUES ($1, $2, $3, $4, $5, $6)
                RETURNING id, name, description, embedding_model, chunk_size, chunk_overlap, separator_type, created_at
            ''', name, description, embedding_model, chunk_size, chunk_overlap, separator_type)
            return dict(row)

    async def get_kb(self, kb_id: int) -> Optional[Dict[str, Any]]:
        """获取知识库"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            row = await conn.fetchrow(
                'SELECT * FROM knowledge_bases WHERE id = $1', kb_id
            )
            return dict(row) if row else None

    async def list_kbs(self) -> List[Dict[str, Any]]:
        """列出所有知识库"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            rows = await conn.fetch('''
                SELECT kb.*,
                       COUNT(DISTINCT d.id) as document_count,
                       COALESCE(SUM(d.chunk_count), 0) as total_chunks
                FROM knowledge_bases kb
                LEFT JOIN documents d ON kb.id = d.kb_id
                GROUP BY kb.id
                ORDER BY kb.updated_at DESC
            ''')
            return [dict(row) for row in rows]

    async def update_kb(self, kb_id: int, **kwargs) -> Optional[Dict[str, Any]]:
        """更新知识库"""
        pool = await self._get_pool()
        allowed_fields = ['name', 'description', 'embedding_model', 'chunk_size', 'chunk_overlap', 'separator_type']
        updates = {k: v for k, v in kwargs.items() if k in allowed_fields}

        if not updates:
            return await self.get_kb(kb_id)

        set_clause = ', '.join([f"{k} = ${i+2}" for i, k in enumerate(updates.keys())])
        values = [kb_id] + list(updates.values())

        async with pool.acquire() as conn:
            row = await conn.fetchrow(f'''
                UPDATE knowledge_bases SET {set_clause}, updated_at = CURRENT_TIMESTAMP
                WHERE id = $1
                RETURNING *
            ''', *values)
            return dict(row) if row else None

    async def delete_kb(self, kb_id: int) -> bool:
        """删除知识库"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            result = await conn.execute('DELETE FROM knowledge_bases WHERE id = $1', kb_id)
            return result == 'DELETE 1'

    async def add_document(
        self,
        kb_id: int,
        filename: str,
        file_type: str,
        file_size: int,
        total_chars: int,
        chunk_count: int,
        clean_settings: Dict,
        chunks: List[Dict]
    ) -> Dict[str, Any]:
        """添加文档及其片段"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            # 插入文档
            doc_row = await conn.fetchrow('''
                INSERT INTO documents (kb_id, filename, file_type, file_size, total_chars, chunk_count, clean_settings)
                VALUES ($1, $2, $3, $4, $5, $6, $7)
                RETURNING id, filename, file_type, file_size, total_chars, chunk_count, created_at
            ''', kb_id, filename, file_type, file_size, total_chars, chunk_count, json.dumps(clean_settings))

            doc_id = doc_row['id']

            # 批量插入片段
            for chunk in chunks:
                await conn.execute('''
                    INSERT INTO document_chunks (doc_id, chunk_index, content, char_count, separator_hint, metadata)
                    VALUES ($1, $2, $3, $4, $5, $6)
                ''', doc_id, chunk['index'], chunk['content'], chunk.get('char_count', 0),
                    chunk.get('separator_hint', ''), json.dumps(chunk.get('metadata', {})))

            return dict(doc_row)

    async def list_documents(self, kb_id: int) -> List[Dict[str, Any]]:
        """列出知识库中的文档"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            rows = await conn.fetch('''
                SELECT * FROM documents WHERE kb_id = $1 ORDER BY created_at DESC
            ''', kb_id)
            return [dict(row) for row in rows]

    async def get_document(self, doc_id: int) -> Optional[Dict[str, Any]]:
        """获取文档详情"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            row = await conn.fetchrow('SELECT * FROM documents WHERE id = $1', doc_id)
            return dict(row) if row else None

    async def get_document_chunks(self, doc_id: int) -> List[Dict[str, Any]]:
        """获取文档的所有片段"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            rows = await conn.fetch('''
                SELECT * FROM document_chunks WHERE doc_id = $1 ORDER BY chunk_index
            ''', doc_id)
            return [dict(row) for row in rows]

    async def delete_document(self, doc_id: int) -> bool:
        """删除文档"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            result = await conn.execute('DELETE FROM documents WHERE id = $1', doc_id)
            return result == 'DELETE 1'

    async def get_all_chunks(self, kb_id: int) -> List[Dict[str, Any]]:
        """获取知识库的所有片段（用于向量检索）"""
        pool = await self._get_pool()
        async with pool.acquire() as conn:
            rows = await conn.fetch('''
                SELECT dc.*, d.filename
                FROM document_chunks dc
                JOIN documents d ON dc.doc_id = d.id
                WHERE d.kb_id = $1
                ORDER BY d.created_at DESC, dc.chunk_index
            ''', kb_id)
            return [dict(row) for row in rows]


# 全局实例
kb_manager = KnowledgeBaseManager()

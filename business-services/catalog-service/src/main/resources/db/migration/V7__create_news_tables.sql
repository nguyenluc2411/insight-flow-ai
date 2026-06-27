CREATE TABLE IF NOT EXISTS news_articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    content JSONB NOT NULL,
    cover_image_url VARCHAR(500),
    author_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS news_ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    news_article_id UUID NOT NULL REFERENCES news_articles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    rating_value INT NOT NULL CHECK (rating_value >= 1 AND rating_value <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (news_article_id, user_id)
);

CREATE INDEX idx_news_articles_status ON news_articles(status);
CREATE INDEX idx_news_articles_published_at ON news_articles(published_at);

-- V1__init_schema.sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    country VARCHAR(100),
    currency_code VARCHAR(10) NOT NULL DEFAULT 'CLP',
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE financial_spaces (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'CLP',
    initial_balance NUMERIC(18, 2) NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE memberships (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES financial_spaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (space_id, user_id)
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE incomes (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES financial_spaces(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    income_date DATE NOT NULL,
    category VARCHAR(100) NOT NULL,
    received_by VARCHAR(255) NOT NULL,
    income_type VARCHAR(50) NOT NULL,
    frequency VARCHAR(50),
    payment_method VARCHAR(100),
    notes TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES financial_spaces(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    due_date DATE NOT NULL,
    category VARCHAR(100) NOT NULL,
    responsible_person VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    expense_type VARCHAR(50) NOT NULL,
    frequency VARCHAR(50),
    payment_method VARCHAR(100),
    receipt_path VARCHAR(500),
    notes TEXT,
    paid_at DATE,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES financial_spaces(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    amount_limit NUMERIC(18, 2) NOT NULL CHECK (amount_limit > 0),
    month_year VARCHAR(7) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (space_id, category, month_year)
);

CREATE TABLE activity_logs (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES financial_spaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_incomes_space_date ON incomes(space_id, income_date);
CREATE INDEX idx_expenses_space_due ON expenses(space_id, due_date);
CREATE INDEX idx_expenses_space_status ON expenses(space_id, status);
CREATE INDEX idx_activity_space ON activity_logs(space_id, created_at DESC);

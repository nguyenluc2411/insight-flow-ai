---
agent: ai-agent
paths:
  - "intelligence-services/ml-service/**"
  - "config-repo/ml-service.yml"
  - "api-contracts/ml-service.yaml"
keywords:
  - "forecast"
  - "demand prediction"
  - "Prophet"
  - "XGBoost"
  - "LSTM"
  - "recommendation"
  - "feature engineering"
  - "model training"
  - "model serving"
  - "ML pipeline"
  - "cold start"
  - "MAPE"
  - "time series"
  - "regression model"
priority: high
---

# AI Routing Rule

This rule routes tasks related to ML/AI services to the `ai-agent`.

## When This Rule Applies

A task should be delegated to `ai-agent` when ANY of the following is true:

1. **File paths match**: Changes within `intelligence-services/**` or related ML configs
2. **Keywords appear**: User mentions forecasting, recommendation, ML model, or feature engineering
3. **Intent matches**: User asks about predictions, model performance, or data pipelines for ML

## Examples That Match

✅ "Improve forecast accuracy for new SKUs"  
✅ "Add a recommendation rule for restock"  
✅ "Why is MAPE so high for this category?"  
✅ "Set up Prophet training pipeline"  
✅ "Build a feature for sales velocity"  
✅ "Cold-start strategy for products with <30 days data"  
✅ "Add Kafka consumer to ingest sales events"  

## Examples That DO NOT Match (Despite Looking Similar)

❌ "Display forecast on dashboard" → frontend repo (not this one)  
❌ "Add forecast field to product entity" → `database-agent` (schema design)  
❌ "Forecast endpoint returns 500" → check if it's ML logic or infrastructure first, may need root coordination  
❌ "Optimize SQL query for sales aggregation" → `database-agent`  

## Cross-Domain Coordination Required When

- **New training data source needed** → request event topic from relevant business service owner
- **Model needs new feature from another service** → coordinate with that service's owner agent
- **ML endpoint added** → update OpenAPI spec, coordinate with `frontend-agent`
- **Performance issue at scale** → may need infrastructure changes (root coordinates)

## Delegation Behavior

When this rule triggers:
1. Root identifies the task as ML-related
2. Root invokes `ai-agent` with:
   - Task description
   - Relevant data/file context (sample data if helpful)
   - Constraint: respect Phase 1 simplicity (no LSTM, no online learning)
3. `ai-agent` works strictly within `intelligence-services/ml-service/`
4. Root verifies:
   - No deep learning introduced without approval
   - Model artifacts NOT committed
   - Tenant isolation maintained
   - Phase 1 boundaries respected

## Prevent Cross-Domain Contamination

The `ai-agent` MUST NOT:
- Modify business service DB schemas directly → request event-based access
- Write directly to business service databases → events only
- Modify gateway/auth/integration code
- Add new Java services (ML services are Python)
- Touch frontend code

If task requires the above, `ai-agent` should:
- Define what data is needed
- Request via Kafka event topic creation (root coordinates)
- Return control to root

## Special: Phase 1 Discipline

ML scope is intentionally minimal for MVP. Any of these should escalate to root:
- Request to use deep learning (LSTM, Transformer)
- Request to add MLflow infrastructure
- Request for real-time online learning
- Request to add GPU infrastructure
- Request for sub-second prediction latency

These may be valid in Phase 2, but require architectural decision, not just agent action.

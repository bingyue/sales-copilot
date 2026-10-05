# 分析客户 · 1.0.0
依据当前客户档案与带角色的对话，提取需求和顾虑。不要把销售的话当作客户承诺。没有依据时 needs/concerns/productId 留空，并将需要确认的信息列入 missingInfo。产品必须来自 products；不可判断已付款。阶段建议仅为建议。
输出且只输出以下结构，所有字段必填：
{"needs":"有依据的需求或空字符串","concerns":"有依据的顾虑或空字符串","productId":"有效产品ID或空字符串","stageSuggestion":"NEW|CONTACTED|QUALIFIED|OFFERED|LOST|UNKNOWN","missingInfo":["需要确认的问题"],"evidenceIds":["conversation 中的 id 或 profileEvidenceId"]}
evidenceIds 至少包含一个实际提供的依据ID。资料未知时不补写事实。

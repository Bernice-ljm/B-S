from generate_report_figures import (
    BLUE,
    BLUE_DARK,
    BORDER,
    INK,
    PALE,
    PALE_BLUE,
    PALE_GREEN,
    PALE_RED,
    Fig,
)


def business_forms():
    f = Fig("三类固定业务：表单结构与提交校验", "页面字段与服务端规则一一对应，草稿允许不完整，提交必须通过完整校验", 372)
    cards = [
        (14, "采购申请", "至少 2 条采购明细", ["标题 · 原因 · 到货日期", "项目 · 预算上限 · 备注", "名称 · 规格 · 数量 · 单位", "预计单价 · 行金额", "至少 2 家报价 / 单一来源说明", "拟选供应商 · 选择理由"]),
        (172, "差旅申请", "系统计算自然日", ["出发地 · 目的地", "开始 / 结束时间", "出差目的 · 同行人员", "交通方式 · 费用预算", "补充附件", "结束时间 ≥ 开始时间"]),
        (330, "报销申请", "至少 3 条费用明细", ["标题 · 说明 · 关联申请", "交通 / 住宿 / 餐饮 / 办公 / 其他", "日期 · 用途 · 精确金额", "每项 1+ 票据", "无票例外 + 原因", "服务端重新计算合计"]),
    ]
    for x, title, badge, lines in cards:
        f.rect(x, 58, 138, 244, "#FFFFFF", BORDER, 5, 1)
        f.rect(x, 58, 138, 36, PALE_BLUE, "#93C5FD", 5, 0.8)
        f.text(x + 12, 80, title, 10, weight=700)
        f.rect(x + 12, 104, 114, 22, PALE_GREEN, "#86C7A5", 11, 0.7)
        f.text(x + 69, 118.5, badge, 7, "middle")
        for i, line in enumerate(lines):
            y = 151 + i * 24
            f.circle(x + 17, y - 2, 2.4, BLUE)
            f.text(x + 25, y, line, 7.1)
    f.band(44, 326, 394, 28, "共同提交规则：身份取会话 ｜ 时间取服务端 ｜ 金额精确计算 ｜ 校验失败定位字段 / 明细行")
    f.save("07-business-form-blueprint")


def template_editor():
    f = Fig("管理员原型：自定义表单与审批流程配置", "以“设备借用”为例，不修改代码即可发布第四类流程", 376)
    f.rect(14, 52, 454, 294, "#FFFFFF", BORDER, 5, 1.1)
    f.rect(14, 52, 454, 34, PALE_BLUE, "#93C5FD", 5, 0.8)
    f.text(28, 74, "模板管理 / 设备借用 / 草稿 v1", 9.5, weight=700)
    f.rect(354, 60, 42, 18, "#FFFFFF", "#93C5FD", 3, 0.7); f.text(375, 72.5, "预览", 7, "middle", BLUE)
    f.rect(404, 60, 50, 18, BLUE, BLUE_DARK, 3, 0.8); f.text(429, 72.5, "校验并发布", 7, "middle", "#FFFFFF")
    f.rect(28, 100, 202, 220, PALE, "#CBD5E1", 4, 0.8)
    f.text(42, 122, "申请表单", 9, weight=700)
    f.text(42, 141, "字段组件", 7, fill="#64748B")
    for i, label in enumerate(["单行文本", "多行文本", "金额 / 数字", "日期", "单选", "附件"]):
        x = 42 + (i % 2) * 84; y = 152 + (i // 2) * 28
        f.rect(x, y, 72, 19, "#FFFFFF", "#CBD5E1", 3, 0.6); f.text(x + 36, y + 13, label, 6.8, "middle")
    f.text(42, 246, "已配置字段", 7, fill="#64748B")
    for i, value in enumerate(["设备名称 *　text", "归还日期 *　date", "借用原因 *　textarea"]):
        y = 258 + i * 18; f.text(48, y, value, 7)
    f.rect(246, 100, 208, 220, "#FFFFFF", "#CBD5E1", 4, 0.8)
    f.text(260, 122, "审批流程（可调整顺序）", 9, weight=700)
    nodes = [(145, "01", "申请人所在部门负责人", "规则自动解析为具体用户"), (207, "02", "设备管理员审核", "指定用户：赵宁")]
    f.line(278, 154, 278, 242, "#93C5FD", 2)
    for y, num, title, sub in nodes:
        f.circle(278, y + 12, 11, BLUE)
        f.text(278, y + 15, num, 6.5, "middle", "#FFFFFF", 700)
        f.rect(300, y, 138, 46, PALE_BLUE, "#93C5FD", 4, 0.8)
        f.text(310, y + 18, title, 7.3, weight=700)
        f.text(310, y + 34, sub, 6.6, fill="#475569")
    f.rect(260, 274, 178, 28, PALE_GREEN, "#86C7A5", 4, 0.7)
    f.text(349, 292, "发布后形成不可变版本；停用只影响新申请", 6.8, "middle")
    f.text(241, 365, "发布校验：字段 key 唯一 ｜ 校验规则合法 ｜ 节点不少于 2 个 ｜ 审批人可解析 ｜ 不允许循环", 7.2, "middle", INK, 700)
    f.save("08-template-editor-prototype")


def list_notification_states():
    f = Fig("列表、统计、通知与交互状态", "基础功能不仅有正常页面，还要覆盖筛选分页、持久通知和失败反馈", 430)
    # 上半部分左右面板统一到底部 y=334，统计内容不再溢出面板。
    f.rect(14, 54, 292, 280, "#FFFFFF", BORDER, 5, 1)
    f.text(28, 76, "我的申请", 9.5, weight=700)
    filters = [(28, 88, 62, "类型：全部"), (96, 88, 62, "状态：审批中"), (164, 88, 82, "时间：本月"), (252, 88, 42, "搜索")]
    for x, y, w, label in filters:
        f.rect(x, y, w, 20, PALE, "#CBD5E1", 3, 0.6); f.text(x + w / 2, y + 13.5, label, 6.4, "middle")
    f.rect(28, 120, 266, 102, "#FFFFFF", "#CBD5E1", 3, 0.7)
    for i, row in enumerate([("PR-2026-018", "采购设备", "审批中"), ("TR-2026-011", "上海差旅", "已通过"), ("BX-2026-009", "差旅报销", "已驳回")]):
        y = 142 + i * 29
        if i: f.line(28, y - 16, 294, y - 16, "#E2E8F0", 0.7)
        f.text(40, y, row[0], 6.8); f.text(126, y, row[1], 6.8); f.text(264, y, row[2], 6.8, "middle", BLUE if i < 2 else "#B42318")
    f.text(28, 243, "共 27 条　每页 10 条", 6.5, fill="#64748B")
    f.text(280, 243, "‹  1  2  3  ›", 6.8, "end")
    f.text(28, 267, "统计口径：本月提交且最终通过", 6.8, weight=700)
    for i, (label, value, width) in enumerate([("采购  ¥42k", 42, 152), ("差旅  ¥18k", 18, 65), ("报销  ¥31k", 31, 112)]):
        y = 286 + i * 17
        f.text(28, y, label, 5.9)
        f.rect(94, y - 9, width, 8, BLUE if i == 0 else "#93C5FD", "none", 2, 0)
    f.rect(322, 54, 146, 280, "#FFFFFF", BORDER, 5, 1)
    f.text(336, 76, "通知中心", 9.5, weight=700); f.text(454, 76, "未读 3", 6.8, "end", BLUE, 700)
    notifications = [(98, "新的采购待办", "刚刚 · 点击进入详情"), (148, "差旅申请已通过", "10 分钟前"), (198, "报销申请被驳回", "昨天 · 查看理由")]
    for y, title, sub in notifications:
        f.circle(338, y, 3, BLUE)
        f.text(348, y - 2, title, 7, weight=700)
        f.text(348, y + 13, sub, 6.2, fill="#64748B")
        f.line(336, y + 25, 454, y + 25, "#E2E8F0", 0.6)
    f.rect(336, 286, 118, 29, PALE_BLUE, "#93C5FD", 4, 0.6)
    f.text(395, 304, "仅本人可读取 / 标记已读", 6.2, "middle", fill="#475569")
    f.text(14, 356, "统一页面状态反馈", 7.2, weight=700)
    states = [(14, "加载", "骨架屏"), (104, "空结果", "清除筛选"), (194, "校验错误", "定位字段"), (284, "操作失败", "保留输入"), (374, "危险操作", "二次确认")]
    for x, title, sub in states:
        fill = PALE_RED if title in ("操作失败", "危险操作") else PALE_BLUE
        stroke = "#E3A1A1" if fill == PALE_RED else "#93C5FD"
        f.card(x, 365, 84, 48, title, (sub,), fill=fill, stroke=stroke, title_size=7.5)
    f.save("09-list-notification-states")


def org_position_er():
    f = Fig("关系模型 A：组织、角色与审批岗位", "角色能力与审批岗位分离；岗位任期解决人员接替及历史解释", 350)
    entities = [
        (18, 60, "DEPARTMENT", ["PK id", "UK name", "status"]),
        (184, 60, "USER", ["PK id", "UK username / email", "department_id FK", "status"]),
        (350, 60, "ROLE", ["PK id", "UK code", "name"]),
        (18, 210, "APPROVAL_POSITION", ["PK id", "department_id FK", "duty", "effective_from / to"]),
        (184, 210, "POSITION_ASSIGNMENT", ["PK id", "position_id FK", "user_id FK", "PRIMARY / BACKUP"]),
        (350, 210, "USER_ROLE", ["user_id FK", "role_id FK", "UK (user_id, role_id)"]),
    ]
    for x, y, title, fields in entities:
        f.rect(x, y, 114, 90, "#FFFFFF", BLUE if title in ("APPROVAL_POSITION", "POSITION_ASSIGNMENT") else BORDER, 4, 1)
        f.rect(x, y, 114, 24, PALE_BLUE, "none", 4, 0)
        f.text(x + 57, y + 16, title, 7.2, "middle", INK, 700)
        for i, field in enumerate(fields): f.text(x + 9, y + 42 + i * 13, field, 6.2)
    f.arrow(132, 105, 184, 105); f.text(158, 97, "1 : N", 6, "middle")
    f.path("M75 150 V190 H75 V210"); f.text(88, 185, "1 : N 岗位", 6)
    f.arrow(132, 255, 184, 255); f.text(158, 247, "1 : N", 6, "middle")
    f.path("M241 150 V190 H241 V210"); f.text(254, 185, "1 : N 任职", 6)
    f.path("M298 105 H326 V255 H350"); f.text(327, 180, "用户多角色", 6, "middle", rotate=90)
    f.arrow(464, 105, 464, 210); f.text(451, 181, "1 : N", 6)
    f.band(43, 321, 396, 22, "唯一规则：同一部门 + 职责 + 有效期内仅 1 名 PRIMARY；历史任期不覆盖，待办转移另记审计")
    f.save("18-org-position-er")


def business_er():
    f = Fig("关系模型 B：申请主记录与三类业务明细", "统一查询字段进入 APPLICATION；采购、差旅、报销分别保存结构化专属数据", 405)
    def entity(x, y, w, title, fields, accent=False):
        f.rect(x, y, w, 80, BLUE if accent else "#FFFFFF", BLUE_DARK if accent else BORDER, 4, 1)
        color = "#FFFFFF" if accent else INK
        f.text(x + w/2, y + 17, title, 7.3, "middle", color, 700)
        for i, field in enumerate(fields): f.text(x + 8, y + 36 + i*12, field, 5.9, fill=color)
    entity(176, 55, 130, "APPLICATION", ["PK id · UK application_no", "applicant_id / department_id FK", "type · status · amount_minor", "template_version_id · source_id"], True)
    entity(12, 170, 108, "PROCUREMENT", ["PK/FK application_id", "reason · expected_date", "budget_minor · chosen_quote"])
    entity(187, 170, 108, "TRAVEL", ["PK/FK application_id", "origin · destination", "start_at · end_at", "duration_days · budget"])
    entity(362, 170, 108, "REIMBURSEMENT", ["PK/FK application_id", "source_application_id", "explanation", "total_amount_minor"])
    entity(12, 292, 108, "PROCUREMENT_ITEM", ["procurement_id FK", "name · spec · qty · unit", "unit_price · amount"])
    entity(132, 292, 108, "SUPPLIER_QUOTE", ["procurement_id FK", "supplier · date · amount", "link / attachment"])
    entity(252, 292, 108, "EXPENSE_ITEM", ["reimbursement_id FK", "type · date · purpose", "amount · no_receipt"])
    entity(372, 292, 98, "ITEM_RECEIPT", ["expense_item_id FK", "attachment_id FK", "UK reimbursement + file"])
    f.path("M241 135 V151 H66 V170"); f.path("M241 135 V170"); f.path("M241 135 V151 H416 V170")
    f.text(74, 160, "1 : 0..1", 6); f.text(249, 160, "1 : 0..1", 6); f.text(424, 160, "1 : 0..1", 6)
    f.arrow(66, 250, 66, 292); f.path("M66 250 V270 H186 V292"); f.arrow(416, 250, 416, 292); f.path("M416 250 V270 H306 V292")
    f.text(241, 392, "提交校验：type 与详情表匹配 ｜ 金额由明细重算 ｜ source 仅允许本人已批准采购/差旅 ｜ 已提交记录不级联删除", 6.7, "middle", INK, 700)
    f.save("19-business-er")


def workflow_evidence_er():
    f = Fig("关系模型 C：流程运行、附件、通知与审计证据", "实例节点保存实际审批人快照；日志与通知以唯一业务键抵抗重复请求", 400)
    boxes = [
        (14, 60, "APPLICATION", ["PK id", "status · row_version"]),
        (132, 60, "WORKFLOW_INSTANCE", ["application_id UK/FK", "template_version_id", "created_at"]),
        (250, 60, "INSTANCE_NODE", ["instance_id FK", "sequence_no", "assignee_id · status"]),
        (368, 60, "APPROVAL_LOG", ["node_id FK", "actor · action · opinion", "UK request_key"]),
        (14, 225, "ATTACHMENT", ["uploader_id", "storage_key", "mime · size"]),
        (132, 225, "ATTACHMENT_BIND", ["application_id FK", "attachment_id FK", "purpose / quote_id"]),
        (250, 225, "NOTIFICATION", ["recipient_id FK", "application_id FK", "UK recipient + event"]),
        (368, 225, "ADMIN_AUDIT", ["actor_id", "target · action", "before / after"]),
    ]
    for x, y, title, fields in boxes:
        f.rect(x, y, 108, 92, "#FFFFFF", BLUE if title in ("INSTANCE_NODE", "APPROVAL_LOG") else BORDER, 4, 1)
        f.rect(x, y, 108, 24, PALE_BLUE, "none", 4, 0)
        f.text(x+54, y+16, title, 6.7, "middle", weight=700)
        for i, field in enumerate(fields): f.text(x+8, y+43+i*13, field, 5.8)
    for x1, x2 in [(122,132),(240,250),(358,368)]: f.arrow(x1,106,x2,106)
    f.text(127,98,"1:1",5.8,"middle"); f.text(245,98,"1:N",5.8,"middle"); f.text(363,98,"1:N",5.8,"middle")
    f.path("M68 152 V198 H186 V225"); f.text(112,190,"1:N 绑定",6)
    f.arrow(122,271,132,271); f.text(127,263,"N:M",5.8,"middle")
    f.path("M68 152 V187 H304 V225"); f.text(258,180,"1:N 状态事件",6)
    f.path("M422 152 V188 H422 V225", dash="4 3"); f.text(410,190,"管理操作",6,"middle",rotate=90)
    f.band(48, 348, 386, 30, "保留策略：申请 / 节点 / 日志 / 通知 / 审计追加保留；附件先解除草稿绑定并延迟清理，不暴露物理路径")
    f.save("20-workflow-evidence-er")


def main():
    for function in [business_forms, template_editor, list_notification_states,
                     org_position_er, business_er, workflow_evidence_er]:
        function()
    print("generated 6 additional figures")


if __name__ == "__main__":
    main()

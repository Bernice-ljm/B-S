from generate_report_figures import (
    BLUE, BLUE_DARK, BORDER, INK, PALE, PALE_BLUE, PALE_GREEN, PALE_RED, Fig
)


def field(f, x, y, w, label, value, required=False):
    f.text(x, y, label + (" *" if required else ""), 6.6, weight=700)
    f.rect(x, y + 6, w, 22, "#FFFFFF", "#CBD5E1", 3, 0.7)
    f.text(x + 7, y + 21, value, 6.6, fill="#475569")


def identity_pages():
    f = Fig("身份入口原型：注册、待确认与个人信息", "权限申请与普通资料编辑分开，客户端不能自授部门负责人、财务或管理员", 382)
    # Registration
    f.rect(12, 52, 142, 296, "#FFFFFF", BORDER, 5, 1)
    f.text(26, 74, "注册账号", 10, weight=700)
    f.text(26, 89, "填写资料并申请加入部门", 6.8, fill="#64748B")
    field(f, 26, 105, 114, "用户名", "bernice_ljm", True)
    field(f, 26, 146, 114, "姓名", "林同学", True)
    field(f, 26, 187, 114, "邮箱", "student@zju.edu.cn", True)
    field(f, 26, 228, 114, "密码", "至少 8 个字符", True)
    field(f, 26, 269, 114, "申请加入部门", "研发部　⌄", True)
    f.rect(26, 316, 114, 22, BLUE, BLUE_DARK, 4, 0.8)
    f.text(83, 331, "提交注册申请", 7, "middle", "#FFFFFF", 700)
    # Pending
    f.rect(170, 52, 142, 296, "#FFFFFF", BORDER, 5, 1)
    f.text(184, 74, "身份确认进度", 10, weight=700)
    f.rect(184, 96, 114, 42, PALE_GREEN, "#86C7A5", 4, 0.7)
    f.text(241, 114, "注册信息已提交", 7.3, "middle", weight=700)
    f.text(241, 128, "账号处于待确认状态", 6.3, "middle")
    f.line(199, 164, 199, 267, "#CBD5E1", 1.4)
    stages = [(166, "✓", "提交申请", "11-15 09:10"), (215, "●", "管理员确认部门", "当前阶段"), (264, "○", "普通员工身份生效", "等待处理")]
    for y, icon, title, sub in stages:
        f.circle(199, y, 8, BLUE if icon != "○" else "#CBD5E1")
        f.text(199, y + 2.5, icon, 6, "middle", "#FFFFFF")
        f.text(215, y - 2, title, 7, weight=700)
        f.text(215, y + 12, sub, 6.2, fill="#64748B")
    f.text(184, 310, "待确认期间不可发起业务或申请高权限角色", 6.2, fill="#B42318")
    # Profile
    f.rect(328, 52, 142, 296, "#FFFFFF", BORDER, 5, 1)
    f.text(342, 74, "个人信息与角色", 10, weight=700)
    f.circle(399, 105, 18, PALE_BLUE, "#93C5FD", 0.8)
    f.text(399, 109, "林", 10, "middle", BLUE, 700)
    f.text(399, 137, "林同学 · 研发部", 7.4, "middle", weight=700)
    f.text(342, 163, "正式角色", 6.6, fill="#64748B")
    f.rect(342, 172, 53, 19, PALE_BLUE, "#93C5FD", 10, 0.6); f.text(368.5, 185, "普通员工", 6.2, "middle")
    f.text(342, 216, "可自行维护", 6.6, fill="#64748B")
    f.text(348, 234, "姓名 · 邮箱 · 密码", 7)
    f.text(342, 263, "需独立申请 / 管理员授予", 6.6, fill="#64748B")
    f.text(348, 281, "转部门 · 业务角色", 7)
    f.rect(342, 307, 114, 22, "#FFFFFF", "#93C5FD", 4, 0.7)
    f.text(399, 322, "查看身份申请历史", 6.8, "middle", BLUE, 700)
    f.text(241, 372, "服务端规则：username / email 唯一 ｜ 密码哈希 ｜ 高权限字段一律忽略 ｜ 授权变更写审计", 7, "middle", INK, 700)
    f.save("10-identity-pages-prototype")


def reimbursement_page():
    f = Fig("报销填写原型：原申请、费用与票据同屏核对", "以结构化费用行驱动金额与票据规则，OCR 不作为基础依赖", 420)
    f.rect(12, 52, 458, 338, "#FFFFFF", BORDER, 5, 1)
    f.text(26, 75, "新建报销申请", 10, weight=700)
    f.rect(410, 61, 42, 18, PALE_GREEN, "#86C7A5", 9, 0.6); f.text(431, 73, "草稿", 6.5, "middle")
    # Left content
    f.rect(26, 92, 286, 280, PALE, "#CBD5E1", 4, 0.7)
    field(f, 40, 110, 120, "报销标题", "杭州项目差旅报销", True)
    field(f, 174, 110, 124, "关联本人已批准申请", "TR-20261105-003　⌄")
    f.rect(40, 157, 258, 43, PALE_BLUE, "#93C5FD", 4, 0.7)
    f.text(50, 174, "杭州项目交流差旅 · 已批准", 7, weight=700)
    f.text(50, 190, "预算 ¥3,000　已报 ¥0　在审 ¥0　本次 ¥2,860", 6.2)
    f.text(40, 220, "费用明细（至少 3 条）", 7.5, weight=700)
    f.rect(40, 229, 258, 21, "#FFFFFF", "#CBD5E1", 2, 0.6)
    for x, label in [(46,"类型 / 日期"),(113,"用途"),(205,"金额"),(251,"票据")]: f.text(x, 243, label, 6.1, weight=700)
    rows = [(267,"交通 11-05","往返高铁","¥800","高铁票.pdf"),(291,"住宿 11-08","酒店住宿","¥1,600","发票.jpg"),(315,"餐饮 11-08","出差餐费","¥460","餐饮票.pdf")]
    for y, t, purpose, amount, receipt in rows:
        f.line(40, y - 12, 298, y - 12, "#E2E8F0", 0.6)
        f.text(46,y,t,5.9); f.text(113,y,purpose,5.9); f.text(205,y,amount,5.9); f.text(251,y,receipt,5.5, "middle", BLUE)
    f.text(40, 347, "□ 无票例外（必须填写原因）", 6.2, fill="#B42318")
    f.text(298, 347, "合计 ¥2,860", 7.5, "end", weight=700)
    # Right receipt pane
    f.rect(326, 92, 130, 280, "#FFFFFF", "#CBD5E1", 4, 0.7)
    f.text(340, 113, "票据核对", 8.5, weight=700)
    f.rect(340, 126, 102, 86, PALE, "#CBD5E1", 3, 0.6)
    f.text(391, 160, "住宿发票.jpg", 7, "middle", weight=700)
    f.text(391, 177, "JPEG / 1.2MB", 6.1, "middle", fill="#64748B")
    f.text(391, 198, "点击受控预览", 6.1, "middle", BLUE)
    for y, label, value in [(235,"对应费用","住宿"),(261,"申报金额","¥1,600"),(287,"上传人","林同学"),(313,"上传时间","11-15 09:22")]:
        f.text(340,y,label,6.1,fill="#64748B"); f.text(442,y,value,6.3,"end",weight=700)
    f.rect(340, 335, 48, 22, "#FFFFFF", "#93C5FD", 4, 0.7); f.text(364,350,"保存草稿",6.5,"middle",BLUE)
    f.rect(394, 335, 48, 22, BLUE, BLUE_DARK, 4, 0.7); f.text(418,350,"提交审批",6.5,"middle","#FFFFFF")
    f.text(241, 410, "提交时服务端重验：关联申请归属与终态 ｜ 明细金额 > 0 ｜ 票据不重复 ｜ 合计重新计算 ｜ 附件访问权", 6.8, "middle", INK, 700)
    f.save("11-reimbursement-form-prototype")


def responsive_approval():
    f = Fig("响应式审批原型：桌面核对与手机触控", "同一 Web 功能按屏幕重排；响应式页面不冒充独立 App / 小程序提分项", 405)
    # Desktop
    f.rect(12, 52, 300, 319, "#FFFFFF", BORDER, 5, 1)
    f.text(26, 74, "桌面端 · 报销审批详情", 9.5, weight=700)
    f.text(26, 91, "BX-20261110-001 · 林同学 / 研发部", 6.5, fill="#64748B")
    f.rect(26, 106, 170, 84, PALE, "#CBD5E1", 4, 0.7)
    f.text(38, 124, "费用与票据", 7.5, weight=700)
    for i, row in enumerate(["交通　¥800　查看票据","住宿　¥1,600　已展开 →","餐饮　¥460　查看票据"]): f.text(38,145+i*17,row,6.4)
    f.rect(206, 106, 92, 84, PALE_BLUE, "#93C5FD", 4, 0.7)
    f.text(252, 124, "当前节点", 6.3, "middle", fill="#64748B")
    f.text(252, 143, "财务审核", 7.5, "middle", weight=700)
    f.text(252, 160, "张敏", 7, "middle")
    f.text(252, 178, "无票例外：0", 6.2, "middle")
    f.rect(26, 204, 272, 66, "#FFFFFF", "#CBD5E1", 4, 0.7)
    f.text(38, 222, "审批历史", 7.5, weight=700)
    f.text(38, 241, "✓ 申请人提交　09:20", 6.4)
    f.text(38, 258, "✓ 部门负责人通过　10:05　费用符合安排", 6.4)
    f.rect(26, 283, 190, 36, "#FFFFFF", "#CBD5E1", 3, 0.7); f.text(36,305,"审批意见（驳回必填）",6.3,fill="#64748B")
    f.rect(224, 283, 34, 36, PALE_RED, "#E3A1A1", 3, 0.7); f.text(241,305,"驳回",6.5,"middle","#B42318")
    f.rect(264, 283, 34, 36, BLUE, BLUE_DARK, 3, 0.7); f.text(281,305,"批准",6.5,"middle","#FFFFFF")
    f.text(26, 344, "大屏：正文、历史、票据核对与操作同屏", 6.3, fill="#64748B")
    # Mobile
    f.rect(334, 52, 136, 319, "#FFFFFF", BORDER, 14, 1.2)
    f.rect(375, 59, 54, 5, "#CBD5E1", "none", 3, 0)
    f.text(402, 83, "报销审批", 8.5, "middle", weight=700)
    f.text(348, 103, "杭州项目交流差旅报销", 6.7, weight=700)
    f.text(348, 119, "财务待审批 · ¥2,860", 6.3, fill=BLUE)
    for i, (title, amount) in enumerate([("交通","¥800"),("住宿","¥1,600"),("餐饮","¥460")]):
        y=136+i*36; f.rect(348,y,108,28,PALE,"#CBD5E1",4,0.6); f.text(356,y+18,title,6.3); f.text(448,y+18,amount,6.3,"end",weight=700)
    f.rect(348, 252, 108, 39, PALE_BLUE, "#93C5FD", 4, 0.6)
    f.text(356, 268, "当前：财务审核 / 张敏", 6.1, weight=700)
    f.text(356, 282, "完整历史与票据按需展开", 5.8)
    f.rect(348, 301, 108, 26, "#FFFFFF", "#CBD5E1", 4, 0.7); f.text(356,318,"审批意见",6.1,fill="#64748B")
    f.rect(348, 336, 50, 25, PALE_RED, "#E3A1A1", 5, 0.7); f.text(373,352,"驳回",6.5,"middle","#B42318")
    f.rect(406, 336, 50, 25, BLUE, BLUE_DARK, 5, 0.7); f.text(431,352,"批准",6.5,"middle","#FFFFFF")
    f.text(241, 396, "小屏：表格转卡片 ｜ 正文不横向滚动 ｜ 主要按钮 ≥ 44px 触控区 ｜ 附件与历史按需展开", 6.8, "middle", INK, 700)
    f.save("12-responsive-approval-prototype")


def vertical_map(f, x, labels, fills):
    for i, (label, fill) in enumerate(zip(labels, fills)):
        y = 63 + i * 63
        f.rect(x, y, 94, 38, fill, BLUE_DARK if fill == BLUE else BORDER, 4, 0.8)
        f.text(x + 47, y + 23, label, 6.8, "middle", "#FFFFFF" if fill == BLUE else INK, 700)
        if i < len(labels) - 1: f.arrow(x + 47, y + 38, x + 47, y + 63)


def traceability_map():
    f = Fig("实现关联：页面动作如何落到接口、服务与数据证据", "用四条代表性链路展示前端、应用服务、事务与持久化之间的内在联系", 383)
    columns = [
        (10, "申请提交", ["表单 / 草稿", "POST .../submit", "ApplicationService", "APPLICATION + INSTANCE", "日志 + 首待办 + 通知"]),
        (129, "审批动作", ["审批详情", "POST .../actions", "WorkflowRuntime", "NODE + APPROVAL_LOG", "下一节点 / 终态通知"]),
        (248, "附件下载", ["票据预览", "GET .../content", "AttachmentService", "BIND → APPLICATION", "鉴权后受控文件流"]),
        (367, "模板发布", ["模板编辑器", "POST .../publish", "TemplateService", "TEMPLATE_VERSION", "审计 + 新申请可用"]),
    ]
    fills = [PALE_BLUE, PALE, BLUE, PALE, PALE_GREEN]
    for x, title, labels in columns:
        f.text(x + 47, 48, title, 7.5, "middle", weight=700)
        vertical_map(f, x, labels, fills)
    f.text(241, 371, "统一原则：Controller 不直接改表 ｜ ctx.userId 来自会话 ｜ 写操作有事务/幂等 ｜ 查询先限定授权范围", 6.8, "middle", INK, 700)
    f.save("22-page-api-data-traceability")


def main():
    for fn in [identity_pages, reimbursement_page, responsive_approval, traceability_map]:
        fn()
    print("generated 4 UI-detail figures")


if __name__ == "__main__":
    main()

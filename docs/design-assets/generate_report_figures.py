from pathlib import Path
from xml.sax.saxutils import escape

import fitz


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "report-figures"
OUT.mkdir(parents=True, exist_ok=True)

FONT = "Microsoft YaHei, Noto Sans CJK SC, Arial, sans-serif"
INK = "#1F2937"
LINE = "#475569"
BLUE = "#2563EB"
BLUE_DARK = "#1D4ED8"
PALE = "#F8FAFC"
BORDER = "#94A3B8"
PALE_BLUE = "#EFF6FF"
PALE_GREEN = "#ECFDF5"
PALE_RED = "#FEF2F2"


class Fig:
    def __init__(self, title, subtitle, height=280):
        self.w = 482
        self.h = height
        self.title = title
        self.parts = [
            '<?xml version="1.0" encoding="UTF-8"?>',
            f'<svg xmlns="http://www.w3.org/2000/svg" width="170mm" height="{height / 482 * 170:.2f}mm" viewBox="0 0 482 {height}" role="img">',
            f'<title>{escape(title)}</title>',
            f'<rect x="0" y="0" width="482" height="{height}" fill="#FFFFFF"/>',
        ]
        self.text(16, 22, title, 14, weight=700)
        self.text(16, 38, subtitle, 7.5)

    def text(self, x, y, value, size=7.5, anchor="start", fill=INK, weight=400, rotate=None):
        transform = f' transform="rotate({rotate} {x} {y})"' if rotate is not None else ""
        self.parts.append(
            f'<text x="{x}" y="{y}" text-anchor="{anchor}" font-family="{FONT}" '
            f'font-size="{size}" font-weight="{weight}" fill="{fill}"{transform}>{escape(value)}</text>'
        )

    def rect(self, x, y, w, h, fill=PALE, stroke=BORDER, rx=5, sw=1, ident=None):
        ident_attr = f' id="{ident}"' if ident else ""
        self.parts.append(
            f'<rect{ident_attr} x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>'
        )

    def circle(self, x, y, r, fill="#E2E8F0", stroke="none", sw=0):
        self.parts.append(f'<circle cx="{x}" cy="{y}" r="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>')

    def line(self, x1, y1, x2, y2, color=LINE, sw=1.35, dash=None):
        dash_attr = f' stroke-dasharray="{dash}"' if dash else ""
        self.parts.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{color}" stroke-width="{sw}"{dash_attr}/>')

    def path(self, d, color=LINE, sw=1.35, fill="none", dash=None):
        dash_attr = f' stroke-dasharray="{dash}"' if dash else ""
        self.parts.append(f'<path d="{d}" fill="{fill}" stroke="{color}" stroke-width="{sw}"{dash_attr}/>')

    def arrow(self, x1, y1, x2, y2, color=LINE, sw=1.6):
        # Orthogonal horizontal or vertical arrows, drawn without SVG marker for renderer compatibility.
        if y1 == y2:
            direction = 1 if x2 > x1 else -1
            tip = x2
            base = x2 - direction * 6
            self.line(x1, y1, base, y2, color, sw)
            self.parts.append(f'<path d="M{base} {y2-4} L{tip} {y2} L{base} {y2+4} Z" fill="{color}" stroke="none"/>')
        elif x1 == x2:
            direction = 1 if y2 > y1 else -1
            tip = y2
            base = y2 - direction * 6
            self.line(x1, y1, x2, base, color, sw)
            self.parts.append(f'<path d="M{x2-4} {base} L{x2} {tip} L{x2+4} {base} Z" fill="{color}" stroke="none"/>')
        else:
            raise ValueError("arrow supports horizontal or vertical edges")

    def card(self, x, y, w, h, title, lines=(), fill=PALE, stroke=BORDER, accent=False, num=None, title_size=9.5):
        if accent:
            fill, stroke = BLUE, BLUE_DARK
        self.rect(x, y, w, h, fill, stroke, 5, 1.1)
        title_fill = "#FFFFFF" if accent else INK
        line_fill = "#FFFFFF" if accent else INK
        if num is not None:
            self.circle(x + 14, y + 14, 8, "#FFFFFF" if accent else "#E2E8F0")
            self.text(x + 14, y + 16.8, str(num), 7.5, "middle", INK)
        title_y = y + (33 if num is not None else 23)
        self.text(x + w / 2, title_y, title, title_size, "middle", title_fill)
        for idx, line in enumerate(lines):
            self.text(x + w / 2, title_y + 18 + idx * 13, line, 7.5, "middle", line_fill)

    def band(self, x, y, w, h, title, fill=PALE_BLUE, stroke="#93C5FD"):
        self.rect(x, y, w, h, fill, stroke, 4, 1)
        self.text(x + w / 2, y + h / 2 + 2.8, title, 7.5, "middle")

    def save(self, name):
        self.parts.append("</svg>")
        svg_path = OUT / f"{name}.svg"
        svg_path.write_text("\n".join(self.parts), encoding="utf-8")
        doc = fitz.open(svg_path)
        pix = doc[0].get_pixmap(matrix=fitz.Matrix(3, 3), alpha=False)
        pix.save(OUT / f"{name}.png")
        pdf = fitz.open("pdf", doc.convert_to_pdf())
        pdf.save(OUT / f"{name}.pdf")


def overview():
    f = Fig("产品全景：统一申请与审批底座", "固定业务与自定义流程共享同一条可追溯链路", 265)
    f.card(15, 68, 86, 62, "员工入口", ("填写申请", "查询进度"))
    f.card(120, 68, 96, 62, "统一申请中心", ("采购 · 差旅", "报销 · 自定义"))
    f.card(235, 62, 108, 74, "流程引擎", ("校验 · 快照", "派单 · 状态机"), accent=True)
    f.arrow(101, 99, 120, 99); f.arrow(216, 99, 235, 99)
    f.card(362, 50, 105, 48, "审批角色", ("负责人 · 财务 · 企业负责人",))
    f.card(362, 112, 105, 48, "站内通知", ("待办 · 结果 · 未读",))
    f.arrow(343, 85, 362, 85); f.arrow(343, 123, 362, 123)
    f.card(120, 184, 96, 48, "管理员", ("用户 · 部门 · 模板",))
    f.card(235, 178, 108, 60, "MySQL", ("业务数据 · 快照", "日志 · 通知"))
    f.card(362, 184, 105, 48, "查询与审计", ("统计 · 历史 · 留痕",))
    f.arrow(168, 184, 168, 148); f.arrow(216, 208, 235, 208); f.arrow(289, 136, 289, 178); f.arrow(343, 208, 362, 208)
    f.save("01-product-overview")


def role_boundary():
    f = Fig("角色、动作与数据边界", "角色决定可执行动作，资源归属与节点分配决定可见数据", 286)
    f.card(18, 65, 132, 118, "普通员工", ("起草 / 提交本人申请", "查看本人进度与附件", "按规则撤回"))
    f.card(175, 65, 132, 118, "审批角色", ("只处理本人当前待办", "负责人：授权部门统计", "财务：指定财务节点", "企业负责人：授权高阶待办"), accent=True)
    f.card(332, 65, 132, 118, "系统管理员", ("维护用户 / 部门 / 角色", "维护流程模板", "管理权 ≠ 审批权", "业务正文需另行授权"))
    f.band(86, 212, 310, 32, "统一服务端权限检查：列表 ｜ 详情 ｜ 审批 ｜ 附件 ｜ 通知")
    f.arrow(84, 183, 84, 212); f.arrow(241, 183, 241, 212); f.arrow(398, 183, 398, 212)
    f.text(241, 270, "任何人不得审批本人申请；非当前审批人不得操作节点", 8, "middle", "#B42318", 700)
    f.save("02-role-boundary")


def business_loop():
    f = Fig("申请与审批业务闭环", "草稿、审批中与终态之间的可审计转换", 320)
    xs = [15, 108, 201, 294, 387]
    titles = ["选择类型", "填写草稿", "提交校验", "审批处理中", "审批结果"]
    lines = [("固定 / 自定义",), ("保存与编辑",), ("服务端重算",), ("顺序节点",), ("通过 / 驳回",)]
    for i, (x, t, ls) in enumerate(zip(xs, titles, lines), 1):
        f.card(x, 58, 80, 66, t, ls, accent=(i == 4), num=i)
        if i < 5: f.arrow(x + 80, 91, xs[i], 91)
    f.card(45, 168, 104, 54, "校验失败", ("返回草稿并定位错误",), fill=PALE_RED, stroke="#E3A1A1")
    f.card(189, 168, 104, 54, "申请人撤回", ("失效待办 · 保留历史",), fill=PALE_BLUE, stroke="#93C5FD")
    f.card(333, 168, 104, 54, "复制重新提交", ("生成新申请并关联原单",), fill=PALE_GREEN, stroke="#86C7A5")
    f.path("M241 124 V146 H97 V168"); f.path("M334 124 V146 H241 V168"); f.path("M427 124 V146 H385 V168")
    f.band(82, 256, 318, 28, "并发控制：撤回与审批最多一个成功；重复请求不重复流转")
    f.save("03-business-loop")


def information_architecture():
    f = Fig("产品信息架构", "普通工作区与管理入口分离，导航围绕任务与申请组织", 300)
    f.card(178, 54, 126, 44, "企业审批工作台", (), accent=True)
    f.line(241, 98, 241, 120); f.line(45, 120, 437, 120)
    items = [(20, "工作台", "统计摘要"), (100, "发起申请", "三类 + 自定义"), (180, "我的申请", "进度与历史"), (260, "我的待办", "当前审批任务"), (340, "通知中心", "未读与跳转"), (420, "管理入口", "配置与审计")]
    for x, title, sub in items:
        cx = x + 1
        f.line(cx, 120, cx, 137)
        f.card(max(4, x - 32), 137, 72, 52, title, (sub,), fill=PALE_BLUE if title == "发起申请" else PALE)
    f.line(421, 189, 421, 213); f.line(335, 213, 457, 213)
    for cx, title in [(335, "用户/部门/角色"), (396, "模板/版本"), (457, "授权统计")]:
        f.line(cx, 213, cx, 228)
        f.card(cx - 28, 228, 56, 42, title, (), title_size=7.5)
    f.save("04-information-architecture")


def dashboard_prototype():
    f = Fig("界面原型：审批工作台", "待办优先，同时保留申请进度、统计与通知入口", 320)
    f.rect(16, 52, 450, 244, "#FFFFFF", BORDER, 5, 1.2)
    f.rect(16, 52, 450, 32, PALE_BLUE, "#93C5FD", 5, 0.8)
    f.text(30, 72, "企业审批工作台", 10, weight=700)
    f.rect(174, 61, 130, 15, "#FFFFFF", "#CBD5E1", 3, 0.8); f.text(183, 72, "搜索申请编号 / 标题", 7, fill="#64748B")
    f.text(420, 72, "通知 3　用户", 7.5, "middle")
    f.rect(16, 84, 86, 212, PALE, "#CBD5E1", 0, 0.8)
    for i, label in enumerate(["工作台", "我的申请", "我的待办  5", "已办事项", "通知中心", "管理入口"]):
        yy = 108 + i * 28
        if i == 0: f.rect(24, yy - 15, 70, 22, PALE_BLUE, "#93C5FD", 3, 0.6)
        f.text(36, yy, label, 7.8)
    f.text(120, 108, "早上好，Bernice", 10, weight=700)
    f.rect(400, 94, 50, 22, BLUE, BLUE_DARK, 4, 1); f.text(425, 108.5, "发起申请", 7.5, "middle", "#FFFFFF")
    for x, big, small in [(120, "5", "我的待办"), (218, "12", "我的申请"), (316, "8", "本月通过")]:
        f.rect(x, 128, 82, 46, PALE, "#CBD5E1", 4, 0.8); f.text(x + 41, 149, big, 13, "middle", BLUE, 700); f.text(x + 41, 165, small, 7, "middle")
    f.text(120, 195, "我的待办", 8.5, weight=700)
    f.rect(120, 204, 330, 66, "#FFFFFF", "#CBD5E1", 3, 0.8)
    f.rect(120, 204, 330, 19, PALE, "#CBD5E1", 3, 0.5)
    cols = [(130, "编号 / 标题"), (230, "类型 / 申请人"), (320, "提交时间"), (390, "金额"), (430, "操作")]
    for x, label in cols: f.text(x, 217, label, 6.8)
    f.text(130, 244, "BX-2026-001　差旅报销", 7); f.text(230, 244, "报销 / 李某", 7); f.text(320, 244, "11-14 09:10", 7); f.text(390, 244, "¥680", 7); f.text(430, 244, "审批", 7, fill=BLUE)
    f.text(120, 286, "最近动态：状态变化与本人可见通知", 7, fill="#64748B")
    f.save("05-dashboard-prototype")


def detail_prototype():
    f = Fig("界面原型：申请详情与审批", "业务正文、附件和审批时间线同屏，操作区只对当前审批人开放", 320)
    f.rect(16, 52, 450, 244, "#FFFFFF", BORDER, 5, 1.2)
    f.text(30, 75, "采购申请 PR-2026-000123", 10, weight=700)
    f.rect(185, 61, 42, 18, PALE_BLUE, "#93C5FD", 9, 0.8); f.text(206, 73.5, "审批中", 7, "middle", BLUE)
    f.text(450, 74, "当前：王某（部门负责人）", 7.5, "end")
    f.line(30, 88, 452, 88, "#CBD5E1", 0.8)
    f.rect(30, 103, 188, 170, PALE, "#CBD5E1", 4, 0.8)
    f.text(44, 123, "业务内容", 9, weight=700)
    for i, label in enumerate(["申请人：李某　研发部", "采购总额：¥12,800.00", "采购明细：显示多行结构", "报价比较：供应商 A / B", "附件：报价单.pdf　截图.png"]):
        f.text(44, 147 + i * 24, label, 7.5)
    f.rect(236, 103, 216, 170, "#FFFFFF", "#CBD5E1", 4, 0.8)
    f.text(250, 123, "审批进度", 9, weight=700)
    timeline = [(145, "已提交", "李某　09:10", BLUE), (174, "部门审核", "王某　处理中", BLUE), (203, "财务审核", "等待前序节点", "#94A3B8"), (232, "负责人审批", "等待前序节点", "#94A3B8")]
    f.line(262, 145, 262, 232, "#CBD5E1", 1)
    for y, title, sub, color in timeline:
        f.circle(262, y, 4, color); f.text(278, y + 2, title, 7.8, weight=700); f.text(345, y + 2, sub, 7)
    f.rect(250, 246, 126, 18, "#FFFFFF", "#CBD5E1", 3, 0.8); f.text(257, 258.5, "审批意见", 7, fill="#64748B")
    f.rect(386, 246, 26, 18, PALE_RED, "#E3A1A1", 3, 0.8); f.text(399, 258.5, "驳回", 7, "middle", "#B42318")
    f.rect(418, 246, 26, 18, BLUE, BLUE_DARK, 3, 0.8); f.text(431, 258.5, "批准", 7, "middle", "#FFFFFF")
    f.save("06-detail-prototype")


def snapshot_design():
    f = Fig("设计亮点：提交时解析并冻结审批路径", "路径快照让历史申请不受后续模板与人员变化影响", 350)
    actors = [(35, "申请人"), (135, "应用服务"), (235, "流程解析器"), (335, "MySQL"), (435, "通知服务")]
    for x, name in actors:
        f.rect(x - 34, 55, 68, 28, PALE, BORDER, 4, 0.8); f.text(x, 73, name, 7.5, "middle", weight=700); f.line(x, 83, x, 238, "#CBD5E1", 0.9, "3 3")
    events = [
        (100, 35, 135, "提交表单 + 版本号"),
        (125, 135, 235, "解析模板 / 规则 / 审批人"),
        (150, 235, 135, "返回有序节点与实际用户"),
        (175, 135, 335, "事务写入申请、快照、待办、日志"),
        (200, 135, 35, "返回申请编号与固定路径"),
        (225, 335, 435, "通知事件（幂等键）"),
    ]
    for y, x1, x2, label in events:
        f.arrow(x1, y, x2, y); f.text((x1+x2)/2, y-5, label, 6.8, "middle")
    f.card(45, 270, 118, 48, "模板版本 v3", ("提交时复制",))
    f.card(182, 262, 118, 64, "申请实例快照", ("节点顺序固定", "规则与人员固定"), accent=True)
    f.card(319, 270, 118, 48, "后续模板 v4", ("只影响新申请",))
    f.arrow(163, 294, 182, 294); f.line(319, 294, 300, 294, "#94A3B8", 1.2, "4 3")
    f.save("13-snapshot-design")


def approval_state():
    f = Fig("审批状态与并发控制", "状态转换由数据库事务串行判定，终态不可再次审批", 300)
    f.card(18, 66, 82, 52, "草稿", ("编辑 / 删除",))
    f.card(130, 60, 104, 64, "审批中", ("当前节点批准", "驳回或申请人撤回"), accent=True)
    f.arrow(100, 92, 130, 92); f.text(115, 84, "提交", 6.8, "middle")
    terminals = [(286, 48, "已通过", "全部节点批准", PALE_GREEN, "#86C7A5"), (286, 115, "已驳回", "理由必填", PALE_RED, "#E3A1A1"), (286, 182, "已撤回", "待办失效", PALE_BLUE, "#93C5FD")]
    for x, y, title, sub, fill, stroke in terminals:
        f.card(x, y, 90, 50, title, (sub,), fill=fill, stroke=stroke)
    f.path("M234 78 H260 V73 H286"); f.path("M234 92 H260 V140 H286"); f.path("M234 106 H250 V207 H286")
    f.card(401, 115, 66, 50, "新申请", ("关联原单",))
    f.arrow(376, 140, 401, 140)
    f.path("M376 207 H388 V140 H401", dash="4 3")
    f.band(54, 246, 374, 28, "FOR UPDATE + 状态条件 + 请求幂等键：重复审批或并发撤回最多一方成功")
    f.save("14-approval-state")


def template_lifecycle():
    f = Fig("自定义流程模板生命周期", "发布产生不可变版本；编辑已发布模板会创建新版本", 292)
    xs = [15, 105, 195, 285, 375]
    titles = ["草稿模板", "配置字段", "配置节点", "发布校验", "不可变版本"]
    subs = ["名称 / 部门", "类型 / 必填", "顺序 / 审批人", "字段与规则", "员工可发起"]
    for i, (x, title, sub) in enumerate(zip(xs, titles, subs)):
        f.card(x, 62, 76, 62, title, (sub,), accent=(i == 4), num=i+1)
        if i < 4: f.arrow(x+76, 93, xs[i+1], 93)
    f.card(70, 176, 100, 52, "校验失败", ("定位字段或节点",), fill=PALE_RED, stroke="#E3A1A1")
    f.card(191, 176, 100, 52, "修改产生 v2", ("旧版本仍可追溯",), fill=PALE_BLUE, stroke="#93C5FD")
    f.card(312, 176, 100, 52, "停用模板", ("不接收新申请",), fill=PALE, stroke=BORDER)
    f.path("M323 124 V150 H120 V176"); f.path("M413 124 V150 H241 V176"); f.path("M451 124 V160 H362 V176")
    f.band(85, 253, 312, 24, "实例始终保存发布版本与实际审批路径快照")
    f.save("15-template-lifecycle")


def reimbursement_flow():
    f = Fig("报销金额与附件闭环", "关联申请、费用明细与票据在服务端统一校验", 286)
    f.card(15, 58, 104, 54, "费用明细", ("类型 · 日期 · 用途 · 金额",))
    f.card(15, 132, 104, 54, "票据资料", ("文件或无票例外原因",))
    f.card(140, 88, 104, 66, "服务端校验", ("关联申请权限 / 状态", "票据归属与重复"), accent=True)
    f.path("M119 85 H130 V108 H140"); f.path("M119 159 H130 V134 H140")
    f.card(265, 88, 92, 66, "精确合计", ("整数分 / Decimal", "同单票据去重"))
    f.arrow(244, 121, 265, 121)
    f.card(378, 58, 89, 54, "审批快照", ("固定路径与金额",))
    f.card(378, 132, 89, 54, "财务审核", ("突出无票例外",))
    f.path("M357 105 H368 V85 H378"); f.path("M422 112 V132")
    f.band(97, 224, 288, 30, "附件预览 / 下载：先校验当前用户对所属报销单的访问权限")
    f.save("16-reimbursement-flow")


def er_model():
    f = Fig("MySQL 领域实体与关系分区", "核心实体按身份权限、模板、申请业务和运行审计四个域组织", 430)
    # Domain containers
    f.rect(14, 52, 108, 330, "#FFFFFF", "#CBD5E1", 5, 1); f.text(24, 70, "身份与权限", 8.5, weight=700)
    f.rect(132, 52, 108, 330, "#FFFFFF", "#CBD5E1", 5, 1); f.text(142, 70, "模板配置", 8.5, weight=700)
    f.rect(250, 52, 108, 330, "#FFFFFF", "#CBD5E1", 5, 1); f.text(260, 70, "申请与业务", 8.5, weight=700)
    f.rect(368, 52, 100, 330, "#FFFFFF", "#CBD5E1", 5, 1); f.text(378, 70, "运行与审计", 8.5, weight=700)
    def entity(x, y, w, title, keys, accent=False):
        fill = BLUE if accent else PALE
        stroke = BLUE_DARK if accent else BORDER
        tf = "#FFFFFF" if accent else INK
        f.rect(x, y, w, 48, fill, stroke, 4, 1)
        f.text(x+w/2, y+17, title, 7.6, "middle", tf, 700)
        f.text(x+w/2, y+34, keys, 6.2, "middle", tf)
    entity(24, 84, 88, "USER", "用户名 · 邮箱 · 密码")
    entity(24, 145, 88, "DEPARTMENT", "名称 · 负责人")
    entity(24, 206, 88, "ROLE", "角色名 · 范围")
    entity(24, 267, 88, "USER_DEPARTMENT", "user_id · dept_id")
    entity(24, 328, 88, "USER_ROLE", "user_id · role_id")
    entity(142, 84, 88, "WORKFLOW_TEMPLATE", "名称 · 启用状态")
    entity(142, 145, 88, "TEMPLATE_VERSION", "版本 · 发布状态")
    entity(142, 206, 88, "TEMPLATE_FIELD", "key · 类型 · 校验")
    entity(142, 267, 88, "TEMPLATE_NODE", "顺序 · 审批规则")
    entity(260, 84, 88, "APPLICATION", "编号 · 状态 · 金额", True)
    entity(260, 145, 88, "采购 / 差旅明细", "结构化专属字段")
    entity(260, 206, 88, "REIMBURSEMENT", "关联申请 · 合计")
    entity(260, 267, 88, "EXPENSE_ITEM", "类型 · 日期 · 金额")
    entity(260, 328, 88, "FIELD_VALUE", "动态字段实例值")
    entity(378, 84, 80, "INSTANCE_NODE", "顺序 · assignee")
    entity(378, 145, 80, "APPROVAL_LOG", "动作 · 意见 · 时间")
    entity(378, 206, 80, "ATTACHMENT", "owner · storage_key")
    entity(378, 267, 80, "NOTIFICATION", "recipient · event_key")
    entity(378, 328, 80, "ADMIN_AUDIT", "actor · target · action")
    # Cross-domain relationships, routed through whitespace between containers.
    f.path("M230 169 H245 V108"); f.arrow(245, 108, 260, 108)
    f.arrow(348, 108, 378, 108); f.text(363, 101, "运行", 6, "middle")
    f.line(348, 108, 363, 108); f.line(363, 108, 363, 291)
    for yy in [169, 230, 291]: f.arrow(363, yy, 378, yy)
    f.band(84, 397, 314, 24, "删除策略：已引用实体停用或软删除；申请、审批日志与审计记录追加保留")
    f.save("17-mysql-er-model")


def architecture():
    f = Fig("技术结构与模块依赖", "模块化单体统一授权与事务边界，MySQL 和附件存储独立持久化", 310)
    layers = [
        (60, "响应式 Web 页面", "员工 / 审批人 / 管理员入口", PALE_BLUE, "#93C5FD"),
        (105, "应用与 API", "认证授权 · 用例编排 · 输入校验", PALE, BORDER),
        (150, "领域模块", "业务规则 · 状态机 · 模板解析 · 权限策略", BLUE, BLUE_DARK),
        (195, "数据访问与事件", "Repository · 迁移 · 通知 / 审计事件", PALE, BORDER),
    ]
    for y, title, sub, fill, stroke in layers:
        f.rect(82, y, 318, 34, fill, stroke, 4, 1)
        tf = "#FFFFFF" if fill == BLUE else INK
        f.text(154, y+21, title, 8.5, "middle", tf, 700); f.text(295, y+21, sub, 7.2, "middle", tf)
    for y in [94, 139, 184]: f.arrow(241, y, 241, y+11)
    f.card(25, 252, 124, 42, "受控附件存储", ("元数据在库 · 文件在卷",))
    f.card(179, 246, 124, 54, "MySQL 8 / InnoDB", ("事务 · 外键 · 唯一约束",), accent=True)
    f.card(333, 252, 124, 42, "Docker Compose", ("应用 · 数据库 · 持久卷",))
    f.arrow(87, 252, 87, 229); f.arrow(241, 246, 241, 229); f.arrow(395, 252, 395, 229)
    f.save("21-technical-architecture")


def test_pyramid():
    f = Fig("验证策略：由规则到完整部署", "越靠上数量越少、范围越大；关键权限与并发路径必须进入集成测试", 290)
    # Trapezoid layers without gradients.
    f.parts.append('<path d="M65 235 L417 235 L380 190 L102 190 Z" fill="#F8FAFC" stroke="#94A3B8" stroke-width="1"/>')
    f.parts.append('<path d="M102 190 L380 190 L344 145 L138 145 Z" fill="#EFF6FF" stroke="#93C5FD" stroke-width="1"/>')
    f.parts.append('<path d="M138 145 L344 145 L307 100 L175 100 Z" fill="#DBEAFE" stroke="#60A5FA" stroke-width="1"/>')
    f.parts.append(f'<path d="M175 100 L307 100 L270 55 L212 55 Z" fill="{BLUE}" stroke="{BLUE_DARK}" stroke-width="1.1"/>')
    f.text(241, 217, "单元测试", 9.5, "middle", weight=700); f.text(241, 229, "金额 · 字段校验 · 状态机 · 审批人解析", 7, "middle")
    f.text(241, 172, "集成测试", 9.5, "middle", weight=700); f.text(241, 184, "MySQL 事务 · 权限 · 附件 · 通知幂等", 7, "middle")
    f.text(241, 127, "端到端测试", 9.5, "middle", weight=700); f.text(241, 139, "提交 → 多角色审批 → 终态 → 查询", 7, "middle")
    f.text(241, 80, "部署验收", 9, "middle", "#FFFFFF", 700); f.text(241, 92, "Compose · 迁移 · 重启持久化", 6.5, "middle", "#FFFFFF")
    f.text(241, 268, "重点证据：越权拒绝 ｜ 重复审批仅一次生效 ｜ 服务重启后记录仍可查询", 7.5, "middle")
    f.save("23-test-pyramid")


def main():
    for fn in [overview, role_boundary, business_loop, information_architecture,
               dashboard_prototype, detail_prototype, snapshot_design, approval_state,
               template_lifecycle, reimbursement_flow, er_model, architecture, test_pyramid]:
        fn()
    print(f"generated 13 figures in {OUT}")


if __name__ == "__main__":
    main()

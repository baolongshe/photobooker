# SDD ledger — plan: docs/superpowers/plans/2026-08-03-redis-geo-nearby.md
Task 1: complete (commits b90093e..c1cf5c9, review clean)
Task 1: minor (deferred): syncFromDb(null) 无防御 NPE — brief 自带，消费端固定传非 null
Task 1: minor (deferred): r.getDistance() 无 null 防御 — includeDistance 语义下 Redis 必返回
Task 1: minor (deferred): 测试未覆盖 remove(null)/add(null,...) 分支
Task 1: note — 误提交 e047316 已 reset；用户暂存标记被清除，工作区内容零损失；后续 task 均需精确路径 commit
Task 1: note — brief 为 Spring Data Redis 2.x API，实际 3.2.5；Task 1 已适配 radius()/RedisGeoCommands；接口签名不变
Task 2: fix round 1/5 (1 addressed, 0 open — 过滤断言空洞; commits 90ccfae..b62b9da)
Task 2: complete (commits c1cf5c9..b62b9da, review clean after 1 fix round)
Task 2: minor (deferred): PhotographerServiceImpl 一行空白行改动（零影响）
Task 2: minor (deferred): import 排序为既有文件风格
Task 3: complete (commits b62b9da..4a45287, review clean)
Task 3: minor (deferred): 与既有 NettyServerStarter 均为 CommandLineRunner 无 @Order，启动顺序未定义（相互独立，Task 6 验证）
Task 4: complete (commits 4a45287..f1239e9, review clean)
Task 4: minor (deferred): 单坐标 null 落入 remove 分支 — brief 语义，回退更安全
Task 4: minor (deferred): getId()==null 分支无专门测试（代码已保证）
Task 4: minor (deferred): updatePhotographer GEO 写失败无补偿 — brief 设计，已知取舍
Task 4: minor (deferred): 回归未含 OrderSchedulerTest/ApplicationTests — 不涉本改动模块
Task 5: complete (commits f1239e9..61909f3, review clean)
Task 5: note — PhotographerController 用户 5 行清理（删 RedisTemplate 死字段/调试调用）被文件级合并进 commit；reviewer 核实为纯死代码，零副作用
Task 5: minor (deferred): @RequestParam 缺参时 Spring 抛 400 不达方法体 — null 检查仅单测层生效，Task 6 验证
Task 5: minor (deferred): 未覆盖边界合法值与默认值生效 — Task 6 验证
Task 6: complete (commits 61909f3..2c8e4a6, review clean)
Task 6: minor (deferred): 报告遗漏首次启动 Netty 8081 端口瞬时失败（二次启动正常，既有组件）
Task 6: minor (deferred): 文档 JSON 示例字段不全（示意性缩写）
Task 6: minor (deferred): 报告 -q 表述细微不一致
ALL TASKS COMPLETE — proceeding to final whole-branch review
Final review: With fixes — C1(updatePhotographer 部分字段更新误删 GEO member) + I1(启动同步容错) + I2(deletePhotographer Redis 硬依赖)
Final fix: commits 2c8e4a6..af96f75, 25/25 tests green
Final re-review: C1/I1/I2 all ADDRESSED, no new breakage
ALL COMPLETE — branch new-branch: b90093e..af96f75 (9 commits)

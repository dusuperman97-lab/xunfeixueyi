-- =============================================
-- 学生画像测试数据 - student_id = 1
-- =============================================

INSERT INTO `student_profile` (
  `student_id`, 
  `username`, 
  `nickname`, 
  `email`, 
  `major`, 
  `grade`, 
  `learning_goal`, 
  `learning_style`, 
  `weakness_points`, 
  `preferred_style`,
  
  -- 知识基础维度 (0-100分)
  `concept_clarity`, 
  `memory_stability`, 
  `transfer_ability`, 
  `structural_integrity`, 
  `knowledge_depth`,
  
  -- 认知风格维度 (0-100分)
  `information_processing`, 
  `knowledge_representation`, 
  `learning_initiative`, 
  `cognitive_control`, 
  `practice_orientation`, 
  `task_management`, 
  `cognitive_style_summary`,
  
  -- 学习投入度维度
  `weekly_study_data`, 
  `avg_daily_study_minutes`, 
  `avg_concentration`,
  
  -- 易错偏好维度
  `error_preferences`, 
  `error_type_count`, 
  `high_frequency_error_count`, 
  `avg_error_rate`,
  
  -- 知识获取偏好维度 (0-100%)
  `text_preference`, 
  `image_preference`, 
  `example_preference`, 
  `video_preference`,
  
  -- 学科兴趣领域维度
  `subject_interests`, 
  `interest_summary`, 
  `recommended_path`,
  
  -- 元数据
  `version`
) VALUES (
  1,                                          -- student_id
  'zhangsan',                                 -- username
  '张三',                                     -- nickname
  'zhangsan@example.com',                     -- email
  '计算机科学与技术',                          -- major
  '2021级本科生',                              -- grade
  '掌握Python编程和人工智能基础知识',           -- learning_goal
  '视觉型学习者',                              -- learning_style
  '["动态规划", "递归算法", "图论"]',          -- weakness_points (JSON数组)
  '实践导向',                                  -- preferred_style
  
  -- 知识基础维度
  75,                                         -- concept_clarity (概念清晰度较高)
  68,                                         -- memory_stability (记忆稳固性中等偏上)
  72,                                         -- transfer_ability (迁移能力良好)
  65,                                         -- structural_integrity (结构完整性中等)
  70,                                         -- knowledge_depth (知识深度良好)
  
  -- 认知风格维度
  80,                                         -- information_processing (偏向整体加工)
  85,                                         -- knowledge_representation (强烈视觉倾向)
  78,                                         -- learning_initiative (主动建构型)
  60,                                         -- cognitive_control (偏反思型)
  82,                                         -- practice_orientation (强工程实践导向)
  55,                                         -- task_management (略偏规划型)
  '该学生属于视觉型学习者，偏好通过图表和实例理解概念。在学习过程中表现出较强的主动性，善于从整体上把握知识结构，但在细节把控上需要加强。倾向于通过实际项目来巩固理论知识。',  -- cognitive_style_summary
  
  -- 学习投入度维度
  '{"monday":{"video_minutes":45,"practice_minutes":30,"concentration":75},"tuesday":{"video_minutes":60,"practice_minutes":45,"concentration":80},"wednesday":{"video_minutes":50,"practice_minutes":35,"concentration":70},"thursday":{"video_minutes":55,"practice_minutes":40,"concentration":78},"friday":{"video_minutes":40,"practice_minutes":25,"concentration":65},"saturday":{"video_minutes":90,"practice_minutes":60,"concentration":85},"sunday":{"video_minutes":70,"practice_minutes":50,"concentration":75}}',  -- weekly_study_data (JSON格式)
  52.14,                                      -- avg_daily_study_minutes (日均学习约52分钟)
  75.43,                                      -- avg_concentration (平均专注度75分)
  
  -- 易错偏好维度
  '[{"error_type":"边界条件处理","frequency":"high","error_rate":0.35,"description":"在数组和循环操作中容易忽略边界条件","tags":["算法","编程"]},{"error_type":"类型转换","frequency":"medium","error_rate":0.22,"description":"在不同数据类型转换时容易出现精度丢失","tags":["基础语法"]},{"error_type":"内存管理","frequency":"low","error_rate":0.15,"description":"对指针和引用理解不够深入","tags":["高级特性"]}]',  -- error_preferences (JSON数组)
  3,                                          -- error_type_count
  1,                                          -- high_frequency_error_count
  0.24,                                       -- avg_error_rate (平均错误率24%)
  
  -- 知识获取偏好维度
  20,                                         -- text_preference (偏文字20%)
  35,                                         -- image_preference (偏图像35%)
  30,                                         -- example_preference (偏示例30%)
  15,                                         -- video_preference (偏视频15%)
  
  -- 学科兴趣领域维度
  '[{"field":"人工智能","score":88,"level":"advanced","icon":"🤖"},{"field":"数据结构","score":75,"level":"intermediate","icon":"📊"},{"field":"Web开发","score":65,"level":"beginner","icon":"🌐"},{"field":"数据库","score":70,"level":"intermediate","icon":"💾"}]',  -- subject_interests (JSON数组)
  '学生对人工智能领域表现出浓厚兴趣，具备较好的数据结构和算法基础。在Web开发和数据库方面处于入门到中级水平，建议继续深化这些领域的学习。',  -- interest_summary
  '[{"stage":1,"title":"Python基础强化","description":"巩固Python语法基础和常用库的使用"},{"stage":2,"title":"数据结构与算法","description":"系统学习常见数据结构和算法设计"},{"stage":3,"title":"机器学习入门","description":"了解机器学习基本概念和常用算法"},{"stage":4,"title":"深度学习实践","description":"掌握深度学习框架并进行项目实战"}]',  -- recommended_path (JSON数组)
  
  -- 元数据
  1                                           -- version
);

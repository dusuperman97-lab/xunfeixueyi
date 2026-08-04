import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/views/layout/AdminLayout.vue'
import StudentLayout from '@/views/layout/StudentLayout.vue'

const _router_ = createRouter({
    history: createWebHistory(),
    routes: getRoutes()
})

function getRoutes() {
    let defaultRoutes = [
        {
            path: '/',
            redirect: '/login'
        },

        /**
         * 学生画像引导页
         * 注意：这里不要放到 StudentLayout 下面。
         * 这样进入画像引导页时，不会显示学生端侧边栏和顶部栏。
         */
        {
            path: '/student/profile-guide',
            name: 'student-profile-guide',
            component: () => import('../views/student/ProfileGuide/index.vue'),
            meta: {
                title: '学生画像引导',
                noProfileGuide: true
            }
        },

        /**
         * 数字人交互页（全屏，无侧边栏/顶部栏）
         */
        {
            path: '/student/digitalHuman/:id',
            name: 'student-digitalHuman-detail',
            component: () => import('../views/student/DigitalHuman/AvatarPanel.vue'),
            meta: {
                title: '数字人教师'
            }
        },

        /**
         * 学生数据画像大屏（全屏，无侧边栏/顶部栏）
         */
        {
            path: '/student/new-profile',
            name: 'student-new-profile',
            component: () => import('../views/student/NewStudentProfile/index.vue'),
            meta: {
                title: '数据画像'
            }
        },

        /**
         * 课程学习工作区（全屏，无侧边栏/顶部栏）
         */
        {
            path: '/student/course/:courseId/workspace',
            name: 'student-course-workspace',
            component: () => import('../views/student/course/Workspace/indexStandalone.vue'),
            meta: {
                title: '课程学习'
            }
        },

        /**
         * 多智能体课程生成工作区（全屏，无侧边栏/顶部栏）
         */
        {
            path: '/student/course/workspace/:taskId',
            name: 'student-course-multi-agent-workspace',
            component: () => import('../views/student/course/MultiAgentWorkspace.vue'),
            meta: {
                title: '多智能体课程生成',
                noProfileGuide: true
            }
        },

        {
            path: '/admin',
            name: 'admin',
            component: AdminLayout,
            redirect: '/admin/home',
            children: [
                {
                    path: 'home',
                    name: 'admin-home',
                    component: () => import('../views/admin/Home.vue')
                },
                {
                    path: 'editCurrentUser',
                    name: 'admin-editCurrentUser',
                    component: () => import('../views/EditCurrentUser.vue')
                },
                {
                    path: 'editPassword',
                    name: 'admin-editPassword',
                    component: () => import('../views/EditPassword.vue')
                },
                {
                    path: 'user',
                    name: 'admin-user',
                    component: () => import('../views/admin/UserManage.vue')
                },
                {
                    path: 'digitalHuman',
                    name: 'admin-digitalHuman',
                    component: () => import('../views/admin/DigitalHumanManage.vue')
                },
                {
                    path: 'course',
                    name: 'admin-course',
                    component: () => import('../views/admin/CourseManage.vue')
                },
                {
                    path: 'questionBank',
                    name: 'admin-questionBank',
                    component: () => import('../views/admin/QuestionBankManage.vue')
                },
                {
                    path: 'agentWorkflow',
                    name: 'admin-agentWorkflow',
                    component: () => import('../views/admin/AgentWorkflowManage.vue')
                },
                {
                    path: 'memberPoints',
                    name: 'admin-memberPoints',
                    component: () => import('../views/admin/MemberPointsManage.vue')
                }
            ]
        },

        {
            path: '/student',
            name: 'student',
            component: StudentLayout,
            redirect: '/student/home',
            children: [
                {
                    path: 'home',
                    name: 'student-home',
                    component: () => import('../views/student/Home.vue')
                },
                {
                    path: 'chat',
                    name: 'student-chat',
                    component: () => import('../views/student/Agent/index.vue'),
                    redirect: { name: 'student-chat-start' },
                    children: [
                        {
                            path: '',
                            name: 'student-chat-start',
                            component: () => import('../views/student/Agent/Start/index.vue')
                        },
                        {
                            path: ':memoryId',
                            name: 'student-chat-conversation',
                            component: () => import('../views/student/Agent/Conversation/index.vue')
                        }
                    ]
                },
                {
                    path: 'path',
                    name: 'student-path',
                    component: () => import('../views/student/LearningPath/index.vue')
                },
                {
                    path: 'resources',
                    name: 'student-resources',
                    component: () => import('../views/student/Resources.vue')
                },
                {
                    path: 'course',
                    name: 'student-course',
                    component: () => import('../views/student/course/index.vue')
                },
                {
                    path: 'courseManage',
                    name: 'student-courseManage',
                    component: () => import('../views/student/course/CourseManage.vue')
                },
                {
                    path: 'knowledge',
                    name: 'student-knowledge',
                    component: () => import('../views/student/KnowledgeBase.vue')
                },
                {
                    path: 'tutoring',
                    name: 'student-tutoring',
                    component: () => import('../views/student/ThinkingMap/index.vue')
                },
                {
                    path: 'evaluation',
                    name: 'student-evaluation',
                    component: () => import('../views/student/evaluation.vue')
                },
                {
                    path: 'Portrait',
                    name: 'student-Portrait',
                    component: () => import('../views/student/StudentProfile/StudentProfile.vue')
                },
                {
                    path: 'QuestionBank',
                    name: 'student-QuestionBank',
                    component: () => import('../views/student/QuestionBank/index.vue')
                },
                {
                    path: 'codeSandbox',
                    name: 'student-codeSandbox',
                    component: () => import('../views/student/course/Workspace/CodeSandbox.vue')
                },
                {
                    path: 'membership',
                    name: 'student-membership',
                    component: () => import('../views/student/Membership.vue')
                },
                {
                    path: 'PPTAgent',
                    name: 'student-PPTAgent',
                    component: () => import('../views/student/PPTAgent/index.vue')
                },
                {
                    path: 'digitalHuman',
                    name: 'student-digitalHuman',
                    component: () => import('../views/student/DigitalHuman/index.vue')
                },
                {
                    path: 'editCurrentUser',
                    name: 'student-editCurrentUser',
                    component: () => import('../views/EditCurrentUser.vue')
                },
                {
                    path: 'editPassword',
                    name: 'student-editPassword',
                    component: () => import('../views/EditPassword.vue')
                }
            ]
        },

        {
            path: '/login',
            name: 'login',
            component: () => import('../views/Login.vue'),
            meta: {
                noProfileGuide: true
            }
        },
        {
            path: '/register',
            name: 'register',
            component: () => import('../views/Register.vue'),
            meta: {
                noProfileGuide: true
            }
        },
        {
            path: '/retrievePassword',
            name: 'front-retrievePassword',
            component: () => import('../views/RetrievePassword.vue'),
            meta: {
                noProfileGuide: true
            }
        }
    ]

    defaultRoutes.push({
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        meta: {
            name: '',
            noProfileGuide: true
        },
        component: () => import('../views/404.vue')
    })

    console.log('getDynamicRoutes', defaultRoutes)

    return defaultRoutes
}

/**
 * 学生画像引导拦截
 *
 * 逻辑：
 * 1. 没登录：不处理
 * 2. 访问 /student 下的页面
 * 3. 不是 /student/profile-guide
 * 4. 没有完成过画像引导
 * 5. 自动跳到 /student/profile-guide
 */
_router_.beforeEach((to, from, next) => {
    /**
     * 这里要根据你项目实际存 token 的 key 来调整。
     * 我这里先兼容几种常见写法。
     */
    const token =
        localStorage.getItem('token') ||
        localStorage.getItem('Authorization') ||
        localStorage.getItem('access_token') ||
        localStorage.getItem('satoken') ||
        localStorage.getItem('userToken')

    /**
     * 是否已经完成画像引导。
     * StudentProfileGuide.vue 里面完成或跳过时会设置：
     * localStorage.setItem('student_profile_guided', '1')
     */
    const profileGuided = localStorage.getItem('student_profile_guided') === '1'

    /**
     * 当前是否访问学生端页面。
     */
    const isStudentPage = to.path.startsWith('/student')

    /**
     * 当前是否就是画像引导页。
     */
    const isProfileGuidePage = to.name === 'student-profile-guide'

    /**
     * 当前路由是否明确不需要画像引导。
     */
    const noProfileGuide = to.meta && to.meta.noProfileGuide

    console.log('====== 学生画像路由守卫 ======')
    console.log('to.path:', to.path)
    console.log('to.name:', to.name)
    console.log('token:', token)
    console.log('profileGuided:', profileGuided)
    console.log('isStudentPage:', isStudentPage)
    console.log('isProfileGuidePage:', isProfileGuidePage)
    console.log('noProfileGuide:', noProfileGuide)
    console.log('=============================')

    /**
     * 没有登录，不处理画像逻辑。
     */
    if (!token) {
        next()
        return
    }

    /**
     * 已登录，访问学生端页面，并且还没完成画像引导，
     * 自动跳到画像引导页。
     */
    if (
        isStudentPage &&
        !isProfileGuidePage &&
        !noProfileGuide &&
        !profileGuided
    ) {
        next({
            name: 'student-profile-guide',
            query: {
                redirect: to.fullPath
            }
        })
        return
    }

    /**
     * 如果已经完成画像，还手动访问 /student/profile-guide，
     * 就直接回学生首页。
     */
    if (isProfileGuidePage && profileGuided) {
        next('/student/home')
        return
    }

    next()
})

export default _router_

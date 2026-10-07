/**
* Every API URL, storage key and role name of the app lives here, so nothing is hard-coded in components or services.
* The host part (http://localhost:8080) comes from environments/environment.ts.
*
* @author Team Lead
*/
import { environment } from "src/environments/environment.prod";
export const API_URL: string = environment.apiUrl;
export const API = {
  // authentication (Suriya)
  REGISTER: '/api/register',
  VERIFY_OTP: '/api/verify-otp',
  RESEND_OTP: '/api/resend-otp',
  INSTRUCTOR_APPLICATION_STATUS: '/api/instructor-application/status',
  LOGIN: '/api/login',
  GOOGLE_LOGIN: '/api/google-login',
  REFRESH: '/api/refresh',
  LOGOUT: '/api/logout',
  FORGOT_PASSWORD: '/api/forgot-password',
  RESET_PASSWORD: '/api/reset-password',
  ADMIN_USERS: '/api/admin/users',
  ADMIN_INSTRUCTORS: '/api/admin/instructors',
  // courses (Sivamuthu, Amogh)
  COURSE: '/api/course',
  COURSE_SEARCH: '/api/course/search',
  COURSE_MINE: '/api/course/mine',
  COURSE_AI_SEARCH: '/api/course-ai/search',
  UPLOAD_IMAGE: '/api/upload/image',
  // customer profile and dashboard (Meruva Lokesh)
  CUSTOMER: '/api/customer',
  CUSTOMER_BY_USER: '/api/customer/user',
  DASHBOARD_CUSTOMER: '/api/dashboard/customer',
  DASHBOARD_ADMIN: '/api/dashboard/admin',
  DASHBOARD_INSTRUCTOR: '/api/dashboard/instructor',
  // video lessons (Lokesh)
  VIDEO: '/api/video',
  VIDEO_BY_COURSE: '/api/video/course',
  // cart (Shoryan)
  CART: '/api/cart',
  CART_BY_CUSTOMER: '/api/cart/customer',
  CART_BY_USER: '/api/cart/user',
  // orders (Tanvi)
  ORDER: '/api/order',
  ORDER_BY_CUSTOMER: '/api/order/customer',
  // reviews (Sumit)
  REVIEW: '/api/review',
  REVIEW_BY_USER: '/api/review/user',
  REVIEW_BY_COURSE: '/api/review/course',
  REVIEW_SUMMARY: '/api/review/summary',
  REVIEW_MY: '/api/review/my'
};
/** localStorage keys (the SRS keeps the JWT in localStorage). */
export const STORAGE = {
  TOKEN: 'token',
  ROLE: 'role',
  USER_ID: 'userId',
  CUSTOMER_ID: 'customerId',
  CART_ID: 'cartId',
  USERNAME: 'username',
  SESSION: 'session'
};
export const ROLES = {
  ADMIN: 'ADMIN',
  INSTRUCTOR: 'INSTRUCTOR',
  CUSTOMER: 'CUSTOMER'
};
/** Paths used by the AuthInterceptor to skip the Bearer header. */
export const PUBLIC_API_PATTERN =
  /\/api\/(login|register|verify-otp|resend-otp|instructor-application\/status|refresh|logout|google-login|forgot-password|reset-password)$/;
export const PAGE_SIZE = 9;

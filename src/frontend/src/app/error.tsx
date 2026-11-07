'use client';
import {Button,Notice} from '@/components/ui';
export default function ErrorPage({reset}:{reset:()=>void}){return <section><h1>Không tải được màn hình</h1><Notice kind="error">Đã xảy ra lỗi khi hiển thị. Không có thao tác nghiệp vụ nào được tự động gửi lại.</Notice><Button onClick={reset}>Thử tải lại</Button></section>;}

import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { API, API_URL } from '../constant';
import { CourseVideo } from '../models/video.model';

/**
 * Video lessons of a course. Uploading sends the file to the server, which saves it in a folder and stores its path in
 * the database. Playing takes two steps: ask for a short-lived signed link (this call carries the login token through
 * the AuthInterceptor), then give that link to a <video> tag, which cannot send headers itself.
 *
 * @author Lokesh
 */
@Injectable({
  providedIn: 'root'
})
export class VideoService {
  public apiUrl = API_URL;
  /** Same limits as the server (the server checks again; this only gives a faster message). */
  readonly maxBytes = 500 * 1024 * 1024;
  readonly allowedTypes = ['video/mp4', 'video/webm'];

  constructor(private http: HttpClient) {}

  /** Returns an error message, or null when the file can be uploaded. */
  validate(file: File): string | null {
    if (!this.allowedTypes.includes(file.type)) { return 'Choose an MP4 or WebM video'; }
    if (file.size > this.maxBytes) { return 'The video is too large. The limit is 500 MB'; }
    return null;
  }

  list(courseId: number | string): Observable<CourseVideo[]> {
    return this.http.get<CourseVideo[]>(`${this.apiUrl}${API.VIDEO_BY_COURSE}/${courseId}`);
  }

  /** Emits upload progress events, then the saved lesson (observe: 'events'). */
  upload(courseId: number | string, file: File, title: string, description?: string, orderIndex?: number): Observable<HttpEvent<CourseVideo>> {
    const body = new FormData();
    body.append('file', file);
    body.append('title', title);
    if (description) { body.append('description', description); }
    if (orderIndex) { body.append('orderIndex', String(orderIndex)); }
    return this.http.post<CourseVideo>(`${this.apiUrl}${API.VIDEO_BY_COURSE}/${courseId}`, body, { reportProgress: true, observe: 'events' });
  }

  update(videoId: number, change: { title: string; description?: string; orderIndex?: number }): Observable<CourseVideo> {
    return this.http.put<CourseVideo>(`${this.apiUrl}${API.VIDEO}/${videoId}`, change);
  }

  delete(videoId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}${API.VIDEO}/${videoId}`);
  }

  /** The full URL to put into a <video> tag. It stops working after a while, so ask again when a lesson is opened. */
  streamUrl(videoId: number): Observable<string> {
    return this.http.post<{ token: string; streamPath: string }>(`${this.apiUrl}${API.VIDEO}/${videoId}/stream-token`, {})
      .pipe(map(res => `${this.apiUrl}${res.streamPath}`));
  }
}

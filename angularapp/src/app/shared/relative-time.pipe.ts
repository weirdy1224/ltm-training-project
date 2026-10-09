import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'relativeTime'
})
export class RelativeTimePipe implements PipeTransform {
  transform(value: string | Date | null | undefined): string {
    if (!value) {
      return '';
    }

    const date = new Date(value);
    if (isNaN(date.getTime())) {
      return '';
    }

    const now = new Date();
    const elapsedSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (elapsedSeconds < 0) {
      return 'just now';
    }

    if (elapsedSeconds < 60) {
      return 'just now';
    }

    const elapsedMinutes = Math.floor(elapsedSeconds / 60);
    if (elapsedMinutes < 60) {
      return `${elapsedMinutes}m ago`;
    }

    const elapsedHours = Math.floor(elapsedMinutes / 60);
    if (elapsedHours < 24) {
      return `${elapsedHours}h ago`;
    }

    const elapsedDays = Math.floor(elapsedHours / 24);
    if (elapsedDays === 1) {
      return 'yesterday';
    }

    if (elapsedDays < 7) {
      return `${elapsedDays}d ago`;
    }

    const elapsedWeeks = Math.floor(elapsedDays / 7);
    if (elapsedWeeks < 4) {
      return `${elapsedWeeks}w ago`;
    }

    return date.toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric'
    });
  }
}


import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AiAssistantService } from './ai-assistant.service';
import { Difficulty, QuizResponse } from '../models/ai.model';

export type GenerationStatus = 'idle' | 'generating' | 'ready' | 'error';

export interface QuizState {
  status: GenerationStatus;
  courseId: number | null;
  courseTitle: string;
  difficulty: Difficulty;
  numQuestions: number;
  quiz?: QuizResponse;
  answers: (number | null)[];
  error: string;
}

const INITIAL_STATE: QuizState = {
  status: 'idle',
  courseId: null,
  courseTitle: '',
  difficulty: 'MEDIUM',
  numQuestions: 5,
  quiz: undefined,
  answers: [],
  error: ''
};

@Injectable({
  providedIn: 'root'
})
export class QuizGenerationService {
  private stateSubject = new BehaviorSubject<QuizState>(INITIAL_STATE);
  public state$: Observable<QuizState> = this.stateSubject.asObservable();

  private requestSub?: Subscription;

  constructor(
    private ai: AiAssistantService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  get currentState(): QuizState {
    return this.stateSubject.getValue();
  }

  get isGenerating(): boolean {
    return this.currentState.status === 'generating';
  }

  /**
   * Starts AI quiz generation in the background.
   */
  startGeneration(courseId: number, courseTitle: string, numQuestions: number, difficulty: Difficulty): void {
    if (this.isGenerating) return;

    this.stateSubject.next({
      ...INITIAL_STATE,
      status: 'generating',
      courseId,
      courseTitle,
      numQuestions,
      difficulty
    });

    this.requestSub = this.ai.generateQuiz(courseId, numQuestions, difficulty).subscribe({
      next: (quiz: QuizResponse) => {
        this.stateSubject.next({
          ...this.currentState,
          status: 'ready',
          quiz,
          answers: quiz.questions.map(() => null),
          error: ''
        });

        this.notifyQuizReady(courseTitle);
      },
      error: (err: any) => {
        const errorMsg = this.extractErrorMessage(err, 'The AI could not generate the quiz. Please try again.');
        this.stateSubject.next({
          ...this.currentState,
          status: 'error',
          error: errorMsg
        });

        this.snackBar.open(`⚠️ Quiz generation failed: ${errorMsg}`, 'Dismiss', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'bottom'
        });
      }
    });
  }

  /**
   * Resets the quiz state back to initial.
   */
  reset(): void {
    if (this.requestSub) {
      this.requestSub.unsubscribe();
    }
    this.stateSubject.next(INITIAL_STATE);
  }

  /**
   * Notification shown when quiz generation finishes
   */
  private notifyQuizReady(courseTitle: string): void {
    const title = courseTitle ? ` for "${courseTitle}"` : '';
    const snackRef = this.snackBar.open(
      `✨ Your AI Quiz${title} is ready!`,
      'Take Quiz',
      {
        duration: 9000,
        horizontalPosition: 'right',
        verticalPosition: 'bottom',
        panelClass: ['quiz-success-snack']
      }
    );

    snackRef.onAction().subscribe(() => {
      this.router.navigate(['/ai-quiz']); // Adjust to your actual quiz route if different
    });
  }

  private extractErrorMessage(err: any, fallback: string): string {
    return [400, 403, 404, 429, 502, 503].includes(err?.status)
      ? (err.error?.message || fallback)
      : fallback;
  }
}
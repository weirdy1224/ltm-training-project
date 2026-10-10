import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { QuizGenerationService } from '../../services/quiz-generation.service';

@Component({
  selector: 'app-quiz-loader',
  templateUrl: './quiz-loader.component.html',
  styleUrls: ['./quiz-loader.component.css']
})
export class QuizLoaderComponent {
  constructor(
    public quizService: QuizGenerationService,
    private router: Router
  ) {}

  goToQuiz(): void {
    this.router.navigate(['/ai-quiz']); // Adjust route if needed
  }

  dismiss(): void {
    this.quizService.reset();
  }
}
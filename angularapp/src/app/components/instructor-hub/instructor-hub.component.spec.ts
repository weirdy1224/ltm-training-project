import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InstructorHubComponent } from './instructor-hub.component';

describe('InstructorHubComponent', () => {
  let component: InstructorHubComponent;
  let fixture: ComponentFixture<InstructorHubComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [InstructorHubComponent]
    });
    fixture = TestBed.createComponent(InstructorHubComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

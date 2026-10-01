import { Course } from './course.model';

describe('Course Model', () => {

  fit('Frontend_Course_model_should_create_an_instance_with_defined_properties', () => {

    const course: Course = {
      courseId: 201,
      courseType: 'Fitness',
      courseImageUrl: 'fitness.jpg',
      courseDetails: 'Advanced fitness training course',
      coursePrice: 5000
    };

    expect(course).toBeTruthy();
    expect(course.courseId).toBe(201);
    expect(course.courseType).toBe('Fitness');
    expect(course.courseImageUrl).toBe('fitness.jpg');
    expect(course.courseDetails).toBe('Advanced fitness training course');
    expect(course.coursePrice).toBe(5000);
  });

});

import { Order } from './order.model';
import { Customer } from './customer.model';
import { Course } from './course.model';

describe('Order Model', () => {

  fit('Frontend_Order_model_should_create_an_instance_with_defined_properties', () => {

    const customer: Customer = {
      customerId: 101,
      customerName: 'John Doe',
      information: 'Regular customer',
      user: {
        userId: 1,
        email: 'john@example.com',
        password: 'password123',
        username: 'johndoe',
        mobileNumber: '9876543210',
        role: 'CUSTOMER'
      }
    };

    const course: Course = {
      courseId: 201,
      courseType: 'Fitness',
      courseImageUrl: 'fitness.jpg',
      courseDetails: 'Advanced fitness training course',
      coursePrice: 5000
    };

    const order: Order = {
      orderId: 301,
      orderPrice: 5000,
      courses: [course],
      customer: customer
    };

    expect(order).toBeTruthy();
    expect(order.orderId).toBe(301);
    expect(order.orderPrice).toBe(5000);
    expect(order.courses).toEqual([course]);
    expect(order.customer).toBe(customer);

    expect(order.courses?.[0].courseId).toBe(201);
    expect(order.courses?.[0].courseType).toBe('Fitness');
    expect(order.courses?.[0].coursePrice).toBe(5000);

    expect(order.customer?.customerId).toBe(101);
    expect(order.customer?.customerName).toBe('John Doe');
  });

});

import { User } from './user.model';

describe('User Model', () => {

  fit('Frontend_User_model_should_create_an_instance_with_defined_properties', () => {

    const user: User = {
      userId: 1,
      email: 'john@example.com',
      password: 'password123',
      username: 'johndoe',
      mobileNumber: '9876543210',
      role: 'CUSTOMER'
    };

    expect(user).toBeTruthy();
    expect(user.userId).toBe(1);
    expect(user.email).toBe('john@example.com');
    expect(user.password).toBe('password123');
    expect(user.username).toBe('johndoe');
    expect(user.mobileNumber).toBe('9876543210');
    expect(user.role).toBe('CUSTOMER');
  });

});

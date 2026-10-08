import {
  Component,
  OnInit,
  NgZone
} from '@angular/core';

import {
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';

import { Router } from '@angular/router';
import { STORAGE } from '../../constant';
import { MatDialog } from '@angular/material/dialog';

import {
  ConfirmDialogComponent
} from '../confirm-dialog/confirm-dialog.component';

import {
  Course
} from '../../models/course.model';

import { CartService } from '../../services/cart.service';
import { CustomerService } from '../../services/customer.service';
import { OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';

declare var Razorpay: any;

@Component({
  selector: 'app-place-order',
  templateUrl: './place-order.component.html',
  styleUrls: ['./place-order.component.css']
})
export class PlaceOrderComponent implements OnInit {

  orderForm: FormGroup;

  courses: Course[] = [];

  errorMessage = '';

  loading = false;

  paymentDone = false;

  constructor(
    private fb: FormBuilder,
    private cartService: CartService,
    private customerService: CustomerService,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private router: Router,
    private dialog: MatDialog,
    private ngZone: NgZone
  ) {

    this.orderForm = this.fb.group({

      name: [
        '',
        [Validators.required]
      ],

      mobileNumber: [
        '',
        [
          Validators.required,
          Validators.pattern(/^[0-9]{10}$/)
        ]
      ],

      information: [
        '',
        [Validators.required]
      ],

      email: [
        '',
        [
          Validators.required,
          Validators.email
        ]
      ],

      totalPrice: [
        {
          value: 0,
          disabled: true
        }
      ]
    });
  }

  get f() {
    return this.orderForm.controls;
  }

  ngOnInit(): void {

    this.cartService.getAllCoursesFromCart().subscribe({

      next: (cart) => {

        this.courses = cart.courses || [];

        this.orderForm.patchValue({
          totalPrice: cart.totalAmount || 0
        });
      },

      error: () => {

        this.errorMessage =
          'Could not load your cart.';
      }
    });

    this.customerService.viewCustomerByUserId().subscribe({

      next: (customer) => {

        this.orderForm.patchValue({

          name: customer.customerName,

          information: customer.information,

          mobileNumber:
            customer.user?.mobileNumber,

          email:
            customer.user?.email
        });
      }
    });
  }

  makePayment(): void {

    if (
      this.orderForm.invalid ||
      this.courses.length === 0 ||
      this.loading
    ) {
      return;
    }

    this.loading = true;

    const amount =
      this.orderForm.getRawValue().totalPrice;

    this.paymentService
      .createOrder(amount)
      .subscribe({

        next: (orderResponse: any) => {

          const options = {

            key: 'rzp_test_TkZ1TuPBVmvHTz',

            amount: orderResponse.amount,

            currency: orderResponse.currency,

            name: 'Online E-Learning Platform',

            description: 'Course Purchase',

            order_id: orderResponse.id,

            prefill: {

              name:
                this.f['name'].value,

              email:
                this.f['email'].value,

              contact:
                this.f['mobileNumber'].value
            },

            theme: {
              color: '#1976d2'
            },

            handler: (response: any) => {

              console.log('PAYMENT SUCCESS');

              this.ngZone.run(() => {

                this.verifyAndSave(
                  response
                );

              });
            }
          };

          const razorpay =
            new Razorpay(options);

          razorpay.open();

          this.loading = false;
        },

        error: () => {

          this.loading = false;

          this.errorMessage =
            'Unable to start payment.';
        }
      });
  }

  private verifyAndSave(
    paymentResponse: any
  ): void {

    console.log('VERIFY START');

    const payload = {

      razorpayOrderId:
        paymentResponse.razorpay_order_id,

      razorpayPaymentId:
        paymentResponse.razorpay_payment_id,

      razorpaySignature:
        paymentResponse.razorpay_signature
    };

    this.paymentService
      .verifyPayment(payload)
      .subscribe({

        next: () => {

          console.log('VERIFY SUCCESS');

          const order = {

            customer: {

              customerId:
                Number(
                  localStorage.getItem(
                    'customerId'
                  )
                )
            },

            courses:
              this.courses.map(c => ({
                courseId:
                  c.courseId
              })),

            orderPrice:
              this.orderForm
                .getRawValue()
                .totalPrice
          };

          this.saveOrder(order);
        },

        error: () => {

          console.log('VERIFY FAILED');

          this.errorMessage =
            'Payment verification failed.';
        }
      });
  }

  private saveOrder(
    order: any
  ): void {

    console.log('SAVE ORDER START');

    this.orderService
      .addOrder(order)
      .subscribe({

        next: () => {

          console.log('ORDER SAVED');

          this.cartService
            .removeAllCourses()
            .subscribe({

              next: () => {

                console.log('CART CLEARED');

                this.showConfirmation();
              },

              error: () => {

                console.log('CART CLEAR FAILED');

                this.showConfirmation();
              }
            });
        },

        error: (err) => {

          console.log('ORDER SAVE FAILED');

          this.errorMessage =
            err.status === 409
              ? (
                  err.error?.message ||
                  'You are already enrolled in one of these courses.'
                )
              : 'Could not save order.';
        }
      });
  }

  private showConfirmation(): void {

    console.log('SHOW CONFIRMATION');

    this.paymentDone = true;

    this.dialog.open(
      ConfirmDialogComponent,
      {
        disableClose: true,
        data: {

          title:
            'Payment was successful!',

          message:
            'You are now enrolled. Find your courses under My Enrollments.',

          confirmText:
            'OK',

          hideCancel: true,

          icon:
            'check_circle'
        }
      }
    )
    .afterClosed()
    .subscribe(() => {

      this.closeConfirmation();
    });
  }

  closeConfirmation(): void {

    this.paymentDone = false;

    this.router.navigate([
      '/my-orders'
    ]);
  }
}
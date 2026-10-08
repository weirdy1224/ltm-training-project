import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';


@Injectable({
    providedIn: 'root'
  })
  export class PaymentService {
  
    private api =
    'https://8080-dabebdaeebcdadbbfacbafdfacaeaeacfdedbcbce.premiumproject.examly.io/api/payment';
  
    constructor(
        private http: HttpClient) {}
  
    createOrder(amount:number) {
  
      return this.http.post(
        `${this.api}/create-order`,
        { amount }
      );
    }
  
    verifyPayment(data:any) {
  
      return this.http.post(
        `${this.api}/verify`,
        data
      );
    }
  }
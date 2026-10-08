import { Injectable } from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class InvoiceService {

  private apiUrl =
  'https://8080-dabebdaeebcdadbbfacbafdfacaeaeacfdedbcbce.premiumproject.examly.io/api/invoice';

  constructor(
    private http: HttpClient
  ) {}

  downloadInvoice(
    orderId: number
  ): Observable<Blob> {

    return this.http.get(

      `${this.apiUrl}/order/${orderId}/pdf`,

      {
        responseType: 'blob'
      }
    );
  }
}
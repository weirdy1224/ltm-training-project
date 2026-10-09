import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
import { InvoiceService }from '../../services/invoice.service';

@Component({
  selector: 'app-order-details',
  templateUrl: './order-details.component.html',
  styleUrls: ['./order-details.component.css']
})
export class OrderDetailsComponent implements OnInit {
  order?: Order;
  errorMessage = '';
  loaded = false;

  constructor(private route: ActivatedRoute, private orderService: OrderService,private invoiceService : InvoiceService) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.orderService.viewOrderById(id).subscribe({
      next: (order) => { this.order = order; this.loaded = true; },
      error: () => { this.errorMessage = 'This order could not be found.'; this.loaded = true; }
    });
  }

  downloadInvoice(): void {

    if (!this.order?.orderId) {
      return;
    }
  
    this.invoiceService 
        .downloadInvoice(
          this.order.orderId
        ) 
        .subscribe({
  
          next: (blob) => {
  
            const fileURL =
              window.URL.createObjectURL(blob);
  
            const link =
              document.createElement('a');
  
            link.href = fileURL;
  
            link.download =
              `invoice-${this.order.orderId}.pdf`;
  
            document.body.appendChild(
              link
            );
  
            link.click();
  
            document.body.removeChild(
              link
            );
  
            window.URL.revokeObjectURL(
              fileURL
            );
          },
  
          error: () => {
  
            this.errorMessage =
              'Unable to download invoice.';
          }
        });
  }
}

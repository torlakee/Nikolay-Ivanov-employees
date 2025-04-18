import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'front';
  selectedFile: File | null = null;
  message = '';
  results: any[] = [];

  onFileChanged(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) { 
      this.selectedFile = input.files[0];
      this.onSubmit(); 
    }
  }

  onSubmit() {
    if (!this.selectedFile) return;

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    fetch('http://localhost:8310/process', {
      method: 'POST',
      body: formData
    })
    .then(async res => {
      if (!res.ok) {
        const errorBody = await res.json();  
        throw new Error(errorBody.error);
      }
      return res.json();
    })
    .then(data => {
      this.results = data;
    })
    .catch(err => {
      this.message = '<div>The processing of your file has failed.</div><div>Reason:</div>';
      if (err instanceof TypeError && err.message.includes('Failed to fetch')) {
        this.message += '<div class="error-reason">Cannot connect to the back-end</div>';
      } else {
        this.message += err.message;
      }
    });
  }
}
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CraftService } from '../../services/craft.service';
import { CategoryService, CategoryItem } from '../../services/category.service';
import { AuthService } from '../../services/auth';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-upload-craft',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './upload-craft.html',
  styleUrl: './upload-craft.css',
})
export class UploadCraft implements OnInit {
  title = '';
  category = '';
  description = '';
  basePrice: number | null = null;
  isLiveForAuction = true;

  categories: CategoryItem[] = [];

  imageFile: File | null = null;
  videoFile: File | null = null;

  imagePreviewUrl = '';
  videoPreviewUrl = '';

  errorMessage = '';
  successMessage = '';

  loading = false;

  constructor(
    private craftService: CraftService,
    private categoryService: CategoryService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (data) => {
        this.categories = data;
        if (this.categories.length > 0) {
          this.category = this.categories[0].name;
        }
      },
      error: (err) => {
        console.error('Failed to load categories:', err);
      },
    });
  }

  // ==========================================
  // IMAGE
  // ==========================================

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];

    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Please select a valid image.';
      return;
    }

    this.errorMessage = '';
    this.imageFile = file;

    this.imagePreviewUrl = URL.createObjectURL(file);
  }

  // ==========================================
  // VIDEO
  // ==========================================

  onVideoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];

    const allowedVideoTypes = ['video/mp4', 'video/webm', 'video/quicktime'];

    if (!allowedVideoTypes.includes(file.type)) {
      this.errorMessage = 'Please upload an MP4, WebM, or MOV video.';
      return;
    }

    // Maximum 100 MB
    if (file.size > 100 * 1024 * 1024) {
      this.errorMessage = 'Video size must be less than 100 MB.';
      return;
    }

    this.errorMessage = '';
    this.videoFile = file;

    this.videoPreviewUrl = URL.createObjectURL(file);
  }

  // ==========================================
  // REMOVE IMAGE
  // ==========================================

  removeImage(): void {
    this.imageFile = null;

    if (this.imagePreviewUrl) {
      URL.revokeObjectURL(this.imagePreviewUrl);
    }

    this.imagePreviewUrl = '';
  }

  // ==========================================
  // REMOVE VIDEO
  // ==========================================

  removeVideo(): void {
    this.videoFile = null;

    if (this.videoPreviewUrl) {
      URL.revokeObjectURL(this.videoPreviewUrl);
    }

    this.videoPreviewUrl = '';
  }

  // ==========================================
  // UPLOAD CRAFT
  // ==========================================

  uploadCraft(): void {
    this.errorMessage = '';
    this.successMessage = '';

    // ==========================================
    // VALIDATION
    // ==========================================

    if (!this.title.trim()) {
      this.errorMessage = 'Please enter your craft title.';
      return;
    }

    if (!this.category) {
      this.errorMessage = 'Please select a category.';
      return;
    }

    if (!this.description.trim()) {
      this.errorMessage = 'Please enter a description.';
      return;
    }

    if (!this.basePrice || this.basePrice <= 0) {
      this.errorMessage = 'Please enter a valid base price.';
      return;
    }

    if (!this.imageFile) {
      this.errorMessage = 'Please upload an image of your craft.';
      return;
    }

    if (!this.videoFile) {
      this.errorMessage = 'Please upload a video showing how you made this craft.';
      return;
    }

    if (!this.authService.isLoggedIn()) {
      this.errorMessage = 'Please login before uploading a craft.';
      this.router.navigate(['/login']);
      return;
    }

    // ==========================================
    // FORM DATA
    // ==========================================

    const formData = new FormData();

    formData.append('title', this.title.trim());
    formData.append('category', this.category);
    formData.append('description', this.description.trim());
    formData.append('basePrice', this.basePrice.toString());
    formData.append('image', this.imageFile);
    formData.append('video', this.videoFile);
    formData.append('isLiveForAuction', this.isLiveForAuction.toString());

    // ==========================================
    // START UPLOAD
    // ==========================================

    this.loading = true;

    this.craftService.uploadCraft(formData).subscribe({
      next: (response) => {
        console.log('Craft uploaded successfully:', response);
        this.loading = false;
        this.successMessage = '🎉 Craft and Craft Reel uploaded successfully!';
        this.toastService.success('🎉 Craft and Reel published to marketplace!');

        // Clear form
        this.title = '';
        this.description = '';
        this.basePrice = null;

        this.removeImage();
        this.removeVideo();

        // Go back to dashboard after 1.5 seconds
        setTimeout(() => {
          this.router.navigate(['/artisan-dashboard']);
        }, 1500);
      },

      error: (error) => {
        console.error('Craft upload error:', error);
        this.loading = false;

        if (error.status === 401) {
          this.errorMessage = 'Your session has expired. Please login again.';
          return;
        }

        if (error.status === 403) {
          this.errorMessage = 'You must enable Artisan/Seller mode before uploading a craft.';
          return;
        }

        if (error.status === 413) {
          this.errorMessage = 'The uploaded file is too large.';
          return;
        }

        this.errorMessage =
          error.error?.message ||
          error.error ||
          (typeof error.error === 'string'
            ? error.error
            : 'Unable to upload craft. Please try again.');
      },
    });
  }
}

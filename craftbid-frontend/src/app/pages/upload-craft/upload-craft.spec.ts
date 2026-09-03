import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadCraft } from './upload-craft';

describe('UploadCraft', () => {
  let component: UploadCraft;
  let fixture: ComponentFixture<UploadCraft>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadCraft],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadCraft);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

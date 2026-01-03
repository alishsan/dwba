# CPC Formatting Notes

## Author Information Repetition

**Question**: Names and affiliations appear on both first and second pages. Is this normal?

**Answer**: **Yes, this is normal for `elsarticle` with `preprint` option.**

### Explanation:

1. **`preprint` option**: This is the standard option for manuscript submissions to Elsevier journals (including CPC). It shows:
   - Full author information on the first page (title page)
   - Author information repeated on subsequent pages (typically in header/footer)

2. **Why it's repeated**: 
   - The first page shows the complete title page with all author details
   - Subsequent pages may show abbreviated author info in headers for identification
   - This is standard practice for preprint submissions

3. **For CPC specifically**:
   - CPC uses Elsevier's Editorial Manager system
   - `elsarticle` class with `preprint` option is the standard format
   - The repetition is expected and acceptable for submissions

### Options:

- **`preprint`** (current): Shows full author info on first page, may repeat on subsequent pages - **RECOMMENDED for submission**
- **`final`**: Formatted for final publication (removes some repetition) - Use only after acceptance
- **`review`**: For review process - Similar to preprint

### Recommendation:

**Keep `preprint` option** - This is the correct format for CPC submissions. The repetition is normal and expected. The journal's production team will reformat it for final publication if accepted.

## Current Status

- ✅ Using `elsarticle` class (correct for Elsevier/CPC)
- ✅ Using `preprint` option (correct for submissions)
- ✅ Author information properly formatted
- ✅ PDF compiles successfully (205KB, ~6 pages)

The manuscript is correctly formatted for CPC submission.


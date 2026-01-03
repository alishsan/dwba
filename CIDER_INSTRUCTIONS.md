# Running verify_paper_calculations.clj in Emacs CIDER

## Method 1: Load and Evaluate File (Recommended)

1. **Open the file in Emacs:**
   ```
   C-x C-f verify_paper_calculations.clj
   ```

2. **Start CIDER REPL (if not already running):**
   ```
   M-x cider-jack-in
   ```
   Or if you have a project.clj:
   ```
   M-x cider-jack-in-clj
   ```

3. **Load the file:**
   - Place cursor anywhere in the file
   - Press: `C-c C-k` (cider-load-file)
   - Or: `C-c C-l` (cider-load-buffer)

4. **The script will run automatically** and print results to the REPL buffer

## Method 2: Evaluate Entire Buffer

1. **Open the file:**
   ```
   C-x C-f verify_paper_calculations.clj
   ```

2. **Make sure CIDER REPL is running**

3. **Evaluate entire buffer:**
   ```
   C-c C-k  (cider-load-file)
   ```

## Method 3: Evaluate Region by Region

1. **Open the file in Emacs**

2. **Select a region** (e.g., one section of code)

3. **Evaluate the region:**
   ```
   C-c C-r  (cider-eval-region)
   ```

## Method 4: Use CIDER's File Evaluation

1. **Open the file**

2. **In the REPL buffer, type:**
   ```clojure
   (load-file "verify_paper_calculations.clj")
   ```
   Or with full path:
   ```clojure
   (load-file "/Users/sanetulla/Development/Clojure/dwba/verify_paper_calculations.clj")
   ```

## Troubleshooting

### If you get "namespace not found" errors:

Make sure you're in the project directory. In CIDER REPL:
```clojure
(require '[functions :refer :all])
```

Or ensure the namespace is loaded first:
```clojure
(load-file "src/functions.clj")
(load-file "verify_paper_calculations.clj")
```

### If CIDER isn't connected:

1. Check if REPL is running: Look for `*cider-repl*` buffer
2. If not, start it: `M-x cider-jack-in`
3. Make sure you're in the project root directory

### Quick Check Commands:

In CIDER REPL, verify functions are available:
```clojure
(require '[functions :refer :all])
(phase-shift-convergence-table 2.0 1 46.23 2.0 0.5 [0.1 0.05 0.01] 10.0)
```

## Recommended Workflow

1. **Open Emacs in project directory:**
   ```bash
   cd /Users/sanetulla/Development/Clojure/dwba
   emacs .
   ```

2. **Start CIDER:**
   ```
   M-x cider-jack-in
   ```

3. **Open verification file:**
   ```
   C-x C-f verify_paper_calculations.clj
   ```

4. **Load and run:**
   ```
   C-c C-k
   ```

5. **View results in REPL buffer:**
   - Switch to REPL: `C-c C-z` (cider-switch-to-repl)
   - Or keep both buffers visible

## Keyboard Shortcuts Reference

| Key | Command | Description |
|-----|---------|-------------|
| `C-c C-k` | cider-load-file | Load current file |
| `C-c C-l` | cider-load-buffer | Load current buffer |
| `C-c C-r` | cider-eval-region | Evaluate selected region |
| `C-c C-e` | cider-eval-last-sexp | Evaluate expression before cursor |
| `C-c C-z` | cider-switch-to-repl | Switch to REPL buffer |
| `C-c M-n` | cider-repl-set-ns | Set namespace in REPL |
| `M-x cider-jack-in` | | Start CIDER REPL |

## Alternative: Run from Terminal

If CIDER gives issues, you can also run from terminal:

```bash
cd /Users/sanetulla/Development/Clojure/dwba
lein repl
```

Then in the REPL:
```clojure
(load-file "verify_paper_calculations.clj")
```


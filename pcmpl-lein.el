;;; pcmpl-lein.el --- pcomplete for Leiningen tasks; works with eshell

;; Copyright (C) 2011 Phil Hagelberg
;;
;; Author: Phil Hagelberg
;; URL: http://github.com/technomancy/leiningen
;; Version: 0.1
;; Keywords: eshell completion
;; Created: 2011-01-15

;; This file is not part of GNU Emacs or Leiningen.

;;; Commentary:

;; Provides completion of leiningen tasks using pcomplete, suitable
;; for eshell. Does not support custom :source-path or :test-path.

;;; License:

;; This program is free software; you can redistribute it and/or
;; modify it under the terms of the GNU General Public License
;; as published by the Free Software Foundation; either version 3
;; of the License, or (at your option) any later version.
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with GNU Emacs; see the file COPYING.  If not, write to the
;; Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
;; Boston, MA 02110-1301, USA.

;;; Code:

(require 'cl)
(require 'pcomplete)
(require 'esh-util)

(defvar pcmpl-lein-tasks-alist nil
  "Cached alist of project roots to task lists.")

(defvar pcmpl-lein-project-root nil)

(defun pcmpl-lein-tasks ()
  (or (cdr (assoc pcmpl-lein-project-root pcmpl-lein-tasks-alist))
      (let* ((help (progn (message "Getting Leiningen task list...")
                     (shell-command-to-string "lein help")))
             (tasks (split-string help "\n"))
             (tasks (subseq tasks 4 -3))
             (tasks (mapcar (lambda (line)
                              (substring line 0 (string-match " " line)))
                            tasks)))
        ;; OHAI MEMOIZE.
        (add-to-list 'pcmpl-lein-tasks-alist
                     (cons pcmpl-lein-project-root tasks))
        tasks)))

(defun pcmpl-lein-namespaces-dir ()
  (let ((task (cadr pcomplete-args)))
    (cond ((equal "test" task) "test")
          ((or (equal "run" task) (equal "compile" task)) "src"))))

(defun pcmpl-lein-transform-filename (file)
  (subst-char-in-string ?/ ?.
                        (substring file (+ (length pcmpl-lein-project-root)
                                           (length namespaces-dir) 1) -4)))

(defun pcmpl-lein-namespaces-in-dir (file)
  (if (not (file-directory-p file))
      (if (string-match "\\.clj$" file)
        (pcmpl-lein-transform-filename file))
    (eshell-flatten-list (mapcar 'pcmpl-lein-namespaces-in-dir
                                 (directory-files file t "^[^\\.]")))))

(defun pcmpl-lein-namespaces ()
  (let ((namespaces-dir (pcmpl-lein-namespaces-dir)))
    (when namespaces-dir
      (pcmpl-lein-namespaces-in-dir namespaces-dir))))

;;;###autoload
(defun pcomplete/lein ()
  (let ((pcmpl-lein-project-root (expand-file-name
                                  (locate-dominating-file
                                   default-directory "project.clj"))))
    (pcomplete-here (pcmpl-lein-tasks))
    (if (not (string= "run" (cadr pcomplete-args)))
        (pcomplete-here (pcmpl-lein-namespaces))
      (pcomplete-here (list "-m"))
      (pcomplete-here (pcmpl-lein-namespaces)))))

(provide 'pcmpl-lein)
;;; pcmpl-lein.el ends here

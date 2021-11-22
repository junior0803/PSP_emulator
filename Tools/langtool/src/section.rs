// Super simplified ini file processor.
// Doesn't even bother with understanding comments.
// Just understands section headings and
// keys and values, split by ' = '.

#[derive(Debug, Clone)]
pub struct Section {
    pub name: String,
    pub title_line: String,
    pub lines: Vec<String>,
}

impl Section {
    pub fn insert_line_if_missing(&mut self, line: &str) -> bool {
        let prefix = if let Some(pos) = line.find(" =") {
            &line[0..pos + 2]
        } else {
            return false;
        };

        // Ignore comments when copying lines.
        if prefix.starts_with('#') {
            return false;
        }
        // Need to decide a policy for these.
        if prefix.starts_with("translators") {
            return false;
        }
        let prefix = prefix.to_owned();

        for iter_line in &self.lines {
            if iter_line.starts_with(&prefix) {
                // Already have it
                return false;
            }
        }

        // Now try to insert it at an alphabetic-ish location.
        let prefix = prefix.to_ascii_lowercase();

        // Then, find a suitable insertion spot
        for (i, iter_line) in self.lines.iter().enumerate() {
            if iter_line.to_ascii_lowercase() > prefix {
                println!("Inserting line {} into {}", line, self.name);
                self.lines.insert(i, line.to_owned());
                return true;
            }
        }

        for i in (0..self.lines.len()).rev() {
            if self.lines[i].is_empty() {
                continue;
            }
            println!("Inserting line {} into {}", line, self.name);
            self.lines.insert(i + 1, line.to_owned());
            return true;
        }

        println!("failed to insert {}", line);
        true
    }

    pub fn comment_out_lines_if_not_in(&mut self, other: &Section) {
        // Brute force (O(n^2)). Bad but not a problem.

        for line in &mut self.lines {
            let prefix = if let Some(pos) = line.find(" =") {
                &line[0..pos + 2]
            } else {
                // Keep non-key lines.
                continue;
            };
            if prefix.starts_with("Font") || prefix.starts_with('#') {
                continue;
            }
            if !other.lines.iter().any(|line| line.starts_with(prefix)) {
                println!("Commenting out: {}", line);
                // Comment out the line.
                *line = "#".to_owned() + line;
            }
        }
    }

    pub fn remove_lines_if_not_in(&mut self, other: &Section) {
        // Brute force (O(n^2)). Bad but not a problem.

        self.lines.retain(|line| {
            let prefix = if let Some(pos) = line.find(" =") {
                &line[0..pos + 2]
            } else {
                // Keep non-key lines.
                return true;
            };
            if prefix.starts_with("Font") || prefix.starts_with('#') {
                return true;
            }
            if !other.lines.iter().any(|line| line.starts_with(prefix)) {
                false
            } else {
                true
            }
        });
    }
}
